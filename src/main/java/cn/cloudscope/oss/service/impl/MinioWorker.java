package cn.cloudscope.oss.service.impl;

import cn.cloudscope.oss.bean.DocumentReturnCodeEnum;
import cn.cloudscope.oss.bean.DocumentUrlResult;
import cn.cloudscope.oss.bean.UploadResult;
import cn.cloudscope.oss.config.MinioConfiguration;
import cn.cloudscope.oss.config.properties.MinioProperties;
import cn.cloudscope.oss.utils.FileUtil;
import cn.cloudscope.oss.service.StorageWorker;
import cn.cloudscope.oss.utils.ImageUtil;
import cn.cloudscope.oss.utils.UUIDUtil;
import cn.cloudscope.oss.utils.VideoUtil;
import com.google.common.collect.Maps;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

import static io.minio.ObjectWriteArgs.MIN_MULTIPART_SIZE;

/**
 * 
 * minio 上传工具
 *
 * @author wupanhua
 * @date 2020-03-03 10:47
 *
 * <pre>
 *              www.cloudscope.cn
 *      Copyright (c) 2019. All Rights Reserved.
 * </pre>
 */
@Slf4j
@Data
@ConditionalOnBean(MinioConfiguration.class)
public class MinioWorker implements StorageWorker {

	/**
	 * 上传文件
	 *
	 * @param inputStream 文件流
	 * @param fileName    文件名
	 * @param folder      目标文件夹
	 * @param thumbnail
	 * @return 文件上传后的路径
	 * @author wenxiaopeng
	 * @date 2021/07/09 12:07
	 **/
	@Override
	public UploadResult upload(InputStream inputStream, String fileName, String folder, boolean thumbnail) {

		File file = new File(UUIDUtil.buildUuid() + "." + FileUtil.getFileSuffix(fileName));
		try {
			if (inputStream.available() <= 0) {
				throw new RuntimeException(DocumentReturnCodeEnum.DOCUMENT_EMPTY.getMsg());
			}
			file.createNewFile();
			IOUtils.copyLarge(inputStream, Files.newOutputStream(file.toPath()));
			UploadResult result = new UploadResult();
			result.setFileName(fileName);
			String path = this.ossPath(folder, file.getName());
			if (thumbnail) {
				String thumbPath = buildThumbnail(path, file);
				result.setThumbnail(thumbPath);
			}
			String upPath = this.doUpload(Files.newInputStream(file.toPath()), path, fileName);
			result.setPhyPath(upPath);
			return result;
		} catch (Exception e) {
			log.error("上传失败", e);
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
		} finally {
			if (file.exists()) {
				file.delete();
			}
		}
		return UploadResult.EMPTY;
	}

	/**
	 * 执行上传
	 *
	 * @param stream     文件流
	 * @param path       远程路径
	 * @param originName
	 * @return filepath
	 * @author wenxiaopeng
	 * @date 2022/7/27 16:07
	 */
	@Override
	public String doUpload(InputStream stream, String path, String originName) {
		if (null != stream) {
			try {
				HashMap<String, String> header = Maps.newHashMap();
				if (StringUtils.isNotBlank(originName)) {
					header.put("Content-Disposition", "attachment;filename=" + originName);
				}
				log.info("开始上传文件(by stream)，stream size: {}", stream.available());
				PutObjectArgs args = PutObjectArgs.builder()
						.bucket(minioProperties.getBucketName())
						.contentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType())
						.extraHeaders(header)
						.object(path)
						.stream(stream, -1, MIN_MULTIPART_SIZE * 10)
						.build();
				ObjectWriteResponse response = minioClient.putObject(args);
				log.info("文件上传完成(by stream): {}", response.object());
				return response.object();
			} catch (Exception e) {
				log.error("上传失败：{}", e.getMessage());
				throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
			} finally {
				IOUtils.closeQuietly(stream);
			}
		}
		return null;
	}

	@Override
	public String doUpload(File file, String path) {
		try {
			log.info("开始上传文件，stream size(by file): {}", Files.size(file.toPath()));
			UploadObjectArgs args = UploadObjectArgs.builder().bucket(minioProperties.getBucketName())
					.contentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType())
					.object(path)
					.filename(file.getAbsolutePath())
					.build();
			ObjectWriteResponse response = minioClient.uploadObject(args);
			log.info("文件上传完成(by file): {}", response.object());
			return response.object();
		} catch (Exception e) {
			log.error("上传失败：{}", e.getMessage());
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
		}
	}

	/**
	 * 生成远程文件路径
	 *
	 * @param folder   目标文件夹
	 * @param fileName 文件名
	 * @return java.lang.String
	 * @author wenxiaopeng
	 * @date 2022/7/27 16:37
	 **/
	private String ossPath(String folder, String fileName) {
		if (StringUtils.isNotBlank(folder)) {
			return folder.endsWith("/") ? (folder + fileName) : (folder + "/" + fileName);
		} else {
			return this.generatePath(fileName);
		}
	}

	/**
	 * 下载文件
	 *
	 * @param key 文件路径
	 * @return java.io.InputStream
	 * @author wenxiaopeng
	 * @date 2021/07/09 12:08
	 **/
	@Override
	public InputStream download(String key) {
		GetObjectArgs getArgs = GetObjectArgs.builder()
				.bucket(minioProperties.getBucketName())
				.object(key)
				.build();
		try {
			return minioClient.getObject(getArgs);
		} catch (Exception e) {
			log.error("下载失败", e);
			throw new RuntimeException(e.getMessage() + key);
		}
	}

	/**
	 * 下载文件
	 *
	 * @param key
	 * @param response
	 * @return java.io.InputStream
	 * @author wangkp
	 * @date 13:28 2022/1/25
	 * [key, response]
	 */
	@Override
	public void download(String key, OutputStream response) {
		try {
			InputStream inputStream = this.download(key);
			int buffer = 1024 * 10;
			byte[] data = new byte[buffer];
			try {
				int read;
				while ((read = inputStream.read(data)) != -1) {
					response.write(data, 0, read);
				}
				response.flush();
				response.close();
			} catch (IOException e) {
				log.error("error info ", e);
			}
		} catch (Exception e) {
			log.error("下载失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
		}
	}

	@Override
	public String backupFile(String originPath, boolean deleteOrigin) {
		try {
			String backup = appendSuffix(originPath, SUFFIX_BACKUP);
			ObjectWriteResponse response = minioClient.copyObject(
					CopyObjectArgs.builder().bucket(minioProperties.getBucketName())
							.object(backup)
							.source(CopySource.builder().bucket(minioProperties.getBucketName()).object(originPath).build())
							.build()
			);
			if (null != response) {
				if (deleteOrigin) {
					this.deleteFile(originPath);
				}
				return response.object();
			}
			return null;
		} catch (Exception e) {
			log.error("复制失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.BACKUP_FAILED.getMsg());
		}
	}

	/**
	 * 获取一个可访问的文件链接
	 *
	 * @param key       文件路径
	 * @param expiresIn 过期时间（秒），默认7天
	 * @return DocumentUrlResult
	 * @author wenxiaopeng
	 * @date 2021/07/09 12:11
	 **/
	@Override
	public DocumentUrlResult getDocumentUrl(String key, int expiresIn) {

		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			String url = this.doGenerateUrl(key, expiresIn);
			DocumentUrlResult result = DocumentUrlResult.builder()
					.expiresIn(expiresIn)
					.url(url)
					.build();
			try {
				if (ImageUtil.isImage(key)) {
					String thumbnailPath = this.doGenerateUrl(ImageUtil.appendSuffixHyphenThumbnail(key), expiresIn);
					result.setThumbnail(thumbnailPath);
				} else if (VideoUtil.isVideo(key)) {
					String thumbnailPath = this.doGenerateUrl(StringUtils.substringBeforeLast(key, ".") + ".jpg", expiresIn);
					result.setThumbnail(thumbnailPath);
				}
			} catch (Exception ignored) {
			}
			return result;
		} catch (MinioException | GeneralSecurityException | IOException e) {
			log.error("下载失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
		}
	}

	private String doGenerateUrl(String key, int expiresIn) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, XmlParserException, ServerException {
		GetPresignedObjectUrlArgs originArgs = GetPresignedObjectUrlArgs.builder()
				.bucket(minioProperties.getBucketName())
				.method(Method.GET)
				.expiry(expiresIn)
				.object(key).build();
		String url = minioClient.getPresignedObjectUrl(originArgs);
		return url;
	}

	/**
	 * 
	 * <创建一个指定有效期的图片访问链接>
	 *
	 * @param path   oss存储路径
	 * @param expire 有效时间（s）
	 * @return void
	 * @author wupanhua
	 * @date 11:30 2020-03-03
	 */
	@Override
	public UploadResult createImgExpireUrl(String path, int expire) {
		String originalImgUrl = this.crateFileExpireUrl(path, expire);
		String hyphenThumbnail = appendSuffix(path, SUFFIX_THUMBNAIL);
		String thumbnailUrl = this.crateFileExpireUrl(hyphenThumbnail, expire);
		return UploadResult.createThumbnailResult(originalImgUrl, thumbnailUrl);
	}

	/**
	 * 上传多个文件
	 *
	 * @param files
	 * @return 上传文件成功后的结果集
	 * @author wupanhua
	 */
	@Override
	public UploadResult uploadMultipleFile(List<File> files) {
		try {
			if (files == null ||files.size() <= 0){
				throw new RuntimeException(DocumentReturnCodeEnum.DOCUMENT_EMPTY.getMsg());
			}
			for (File file : files) {
				this.upload(file);
			}
		} catch (Exception e) {
			log.error("多文件上传失败" + e, e);
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
		}
		return null;
	}

	/**
	 * 
	 * <创建一个指定有效期的数据访问链接>
	 *
	 * @param path   存储路径
	 * @param expire 有效时间（s）
	 * @return void
	 * @throws Exception 文件创建异常
	 * @author wupanhua
	 * @date 11:30 2020-03-03
	 */
	@Override
	public String crateFileExpireUrl(String path, int expire) {
		if (StringUtils.isBlank(path)) {
			return null;
		}
		GetPresignedObjectUrlArgs originArgs = GetPresignedObjectUrlArgs.builder()
				.bucket(minioProperties.getBucketName())
				.method(Method.GET)
				.expiry(expire)
				.object(path).build();
		try {
			return minioClient.getPresignedObjectUrl(originArgs);
		} catch (InsufficientDataException | ErrorResponseException | InternalException | InvalidKeyException |
		         InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException |
		         ServerException e) {
			log.error("error info ", e);
		}
		return null;
	}

	/**
	 * 根据路径删除文件
	 * <>
	 *
	 * @param path 1
	 * @return boolean
	 * @author songcx
	 * @date 14:32 2021/2/2
	 **/
	@Override
	public boolean deleteFile(String path) {
		try {
			if (StringUtils.isNotBlank(path)) {
				minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioProperties.getBucketName()).object(path).build());
			}
		} catch (MinioException | GeneralSecurityException | IOException e) {
			log.error("下载失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
		}
		return true;
	}

	/**
	 * minio客户端
	 */
	private final MinioClient minioClient;

	private final MinioProperties minioProperties;

	public MinioWorker(MinioClient minioClient, MinioProperties minioProperties) {
		this.minioClient = minioClient;
		this.minioProperties = minioProperties;
		try {
			if (minioProperties.getEndPoint().startsWith("https")) {
				this.minioClient.ignoreCertCheck();
			}
			BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build();
			if (!minioClient.bucketExists(bucketExistsArgs)) {
				MakeBucketArgs bucketArgs = MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build();
				minioClient.makeBucket(bucketArgs);
			}
		} catch (Exception e) {
			log.error("初始化minio worker异常", e);
		}
	}

}
