package cn.cloudscope.service.Impl;

import cn.cloudscope.bean.DocumentReturnCodeEnum;
import cn.cloudscope.bean.DocumentUrlResult;
import cn.cloudscope.bean.YKUPResult;
import cn.cloudscope.config.MinioConfiguration;
import cn.cloudscope.config.properties.MinioProperties;
import cn.cloudscope.service.StorageWorker;
import cn.cloudscope.utils.DateUtil;
import cn.cloudscope.utils.ImageUtil;
import cn.cloudscope.utils.PathUtil;
import cn.cloudscope.utils.UUIDUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
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
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
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
	 * minio客户端
	 */
	private final MinioClient minioClient;

	private MinioProperties minioProperties;

	public MinioWorker(MinioClient minioClient, MinioProperties minioProperties) {
		this.minioClient = minioClient;
		this.minioProperties = minioProperties;
		try {
			BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build();
			if (!minioClient.bucketExists(bucketExistsArgs)) {
				MakeBucketArgs bucketArgs = MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build();
				minioClient.makeBucket(bucketArgs);
			}
		} catch (NoSuchAlgorithmException | IOException | InvalidKeyException | ErrorResponseException | InvalidResponseException | XmlParserException | InsufficientDataException | InternalException | ServerException e) {
			log.error("初始化minio worker异常", e);
		}
	}

	/**
	 * 上传文件
	 *
	 * @param inputStream 文件流
	 * @param fileName    文件名
	 * @param folder      目标文件夹
	 * @return 文件上传后的路径
	 * @author wenxiaopeng
	 * @date 2021/07/09 12:07
	 **/
	@Override
	public YKUPResult upload(InputStream inputStream, String fileName, String folder) {
		try {
			if(inputStream.available() <= 0) {
				throw new RuntimeException(DocumentReturnCodeEnum.DOCUMENT_EMPTY.getMsg());
			}
			String path;
			if(StringUtils.isNotBlank(folder)) {
				path = folder.endsWith("/") ? (folder + fileName) : (folder + "/" + fileName);
			} else {
				path = this.generatePath(fileName);
			}
			PutObjectArgs putObjectArgs = PutObjectArgs.builder().bucket(minioProperties.getBucketName())
					.object(path)
					.stream(inputStream, -1, PutObjectArgs.MIN_MULTIPART_SIZE)
					.contentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType())
					.build();
			minioClient.putObject(putObjectArgs);
			YKUPResult result = YKUPResult.createResult(path, fileName);
			return result;
		} catch (MinioException | GeneralSecurityException | IOException e) {
			log.error("上传失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
		}
	}

	/**
	 * Description:
	 * <上传文件>
	 * @author wkp
	 * @date 17:19 2022/1/27
	 * @param file
	 * @return cn.cloudscope.bean.YKUPResult
	 **/
	@Override
	public YKUPResult uploadSingleFile(File file) {
		try (FileInputStream fileOutPutStream = new FileInputStream(file)) {
			return this.uploadSingleFile(fileOutPutStream, file.length(), file.getName());
		} catch (Exception e) {
			log.error("minio上传文件异常", e);
		}
		return null;
	}

	/**
	 * 上传单文件
	 * @param inputStream 需要上传的文件流
	 * @return 上传成功后返回的结果
	 * @author wupanhua
	 */
	@Override
	public YKUPResult uploadSingleFile(InputStream inputStream) {
		// 流转换成文件，怎么知道是Excel，jpg
		return null;
	}

	/**
	 * Description:
	 * <上传文件>
	 * @param inputStream 文件流
	 * @param size        文件大小
	 * @return cn.cloudscope.bean.YKUPResult
	 * @author wupanhua
	 * @date 11:14 2020-03-03
	 */
	@Override
	public YKUPResult uploadSingleFile(InputStream inputStream, long size, String fileName) throws Exception {

		//将文件名变为uuid 防止重复
		String[] strs = fileName.split("\\.");
		strs[0] = UUIDUtil.get32UUID();
		fileName = StringUtils.join(".", strs);

		// 将文件存储到本地
		File file = new File(fileName);
		if (!file.exists()) {
			boolean newFile = file.createNewFile();
			log.debug("创建文件状态: {}", newFile);
			if (!newFile) {
				throw new RuntimeException("file system permission denied");
			}
		}
		IOUtils.copy(inputStream, new FileOutputStream(file));
		Map<String, String> headerMap = new HashMap<>(1);
		headerMap.put("Content-Type", "application/octet-stream");
		String path = DateUtil.format(new Date(), DateUtil.DateStyle.YEARMONTHDAY) + "/" + PathUtil.generatePath(UUIDUtil.get32UUID()) + "/" + fileName;
		PutObjectArgs putObjectArgs = PutObjectArgs.builder().headers(headerMap)
				.bucket(minioProperties.getBucketName())
				.object(path).contentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType())
				.stream(new FileInputStream(file), file.length(), ObjectWriteArgs.MIN_MULTIPART_SIZE).build();

		minioClient.putObject(putObjectArgs);
		YKUPResult result = YKUPResult.createResult(path, fileName);

		if (ImageUtil.isImageType(file)) {
			// 构建缩略图
			String thumbnailUri = this.buildThumbnail(path, file);
			result.setThumbnail(thumbnailUri);
		}
		putObjectArgs.stream().close();//一定要关闭，否则文件删除不掉的！
		// 删除文件
		if (file.delete()) {
			log.info("本地文件[{}]未成功删除，可能造成存储浪费", file.getAbsolutePath());
		}
		return result;
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
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
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
				log.error("error info ",e);
			}
		}catch (Exception e){
			log.error("下载失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
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

		if(expiresIn < 0 || expiresIn > minioProperties.getExpiresIn().getSeconds()) {
			expiresIn = (int) minioProperties.getExpiresIn().getSeconds();
		}
		try {
			GetPresignedObjectUrlArgs originArgs = GetPresignedObjectUrlArgs.builder()
					.bucket(minioProperties.getBucketName())
					.method(Method.GET)
					.expiry(expiresIn)
					.object(key).build();
			String url = minioClient.getPresignedObjectUrl(originArgs);
			DocumentUrlResult result = DocumentUrlResult.builder()
					.expiresIn(expiresIn)
					.url(url)
					.build();
			try {
				if(ImageUtil.isImageType(key)) {
					GetPresignedObjectUrlArgs thumbnail = GetPresignedObjectUrlArgs.builder()
							.bucket(minioProperties.getBucketName())
							.method(Method.GET)
							.expiry(expiresIn)
							.object(ImageUtil.appendSuffixHyphenThumbnail(key)).build();
					String thumbnailPath = minioClient.getPresignedObjectUrl(thumbnail);
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

	/**
	 * Description:
	 * <创建一个指定有效期的图片访问链接>
	 * @author wupanhua
	 * @date 11:30 2020-03-03
	 * @param path oss存储路径
	 * @param expire 有效时间（s）
	 * @return void
	 */
	@Override
	public YKUPResult createImgExpireUrl(String path, int expire){
		String originalImgUrl = this.crateFileExpireUrl(path, expire);
		String hyphenThumbnail = appendSuffixHyphenThumbnail(path);
		String thumbnailUrl = this.crateFileExpireUrl(hyphenThumbnail, expire);
		return YKUPResult.createThumbnailResult(originalImgUrl, thumbnailUrl);
	}

	/**
	 * Description:
	 * <上传文件>
	 *
	 * @param path 存储文件的路径
	 * @author wupanhua
	 * @date 11:14 2020-03-03
	 */
	@Override
	public String buildThumbnail(String path, File file) {
		try {
			BufferedImage bufferedImage = Thumbnails.of(file).scale(1d).asBufferedImage();
			// 图片高度
			int height = bufferedImage.getHeight();
			// 计算缩放比列
			double rate = Math.round((200d / height) * 100) * 0.01d;

			// 将图片进行缩小处理,并对文件加入后缀名"-thumbnail"
			Thumbnails.of(file).scale(rate).outputQuality(0.7).toFiles(Rename.SUFFIX_HYPHEN_THUMBNAIL);
			String tumbnailLocate = appendSuffixHyphenThumbnail(file.getAbsolutePath());
			File thumbnail = new File(tumbnailLocate);

			// 将缩略图上传到文件同目录
			try (FileInputStream fileInputStream = new FileInputStream(thumbnail)) {
				String thumbnailUri = appendSuffixHyphenThumbnail(path);
				Map<String, String> headerMap = new HashMap<>(1);
				headerMap.put("Content-Type", "application/octet-stream");

				PutObjectArgs putObjectArgs = PutObjectArgs.builder().headers(headerMap)
						.bucket(minioProperties.getBucketName())
						.object(thumbnailUri)
						.stream(fileInputStream, thumbnail.length(), ObjectWriteArgs.MIN_MULTIPART_SIZE).build();

				minioClient.putObject(putObjectArgs);
				fileInputStream.close();
				// 删除本地缩略图文件
				if (thumbnail.delete()) {
					log.debug("{}删除成功", thumbnailUri);
				}
				return thumbnailUri;
			}

		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException | InvalidResponseException | ErrorResponseException | InternalException | InsufficientDataException | ServerException | XmlParserException e) {
			log.error("生成缩略图异常", e);
		}

		return null;
	}

	/**
	 * Description:
	 * <添加文件后缀>
	 *
	 * @param fileName 文件名（可以包含路径）
	 * @author wupanhua
	 * @date 11:14 2020-03-03
	 */
	@Override
	public String appendSuffixHyphenThumbnail(String fileName) {
		String newFileName = "";

		int indexOfDot = fileName.lastIndexOf('.');

		if (indexOfDot != -1) {
			newFileName = fileName.substring(0, indexOfDot);
			newFileName += "-thumbnail";
			newFileName += fileName.substring(indexOfDot);
		} else {
			newFileName = fileName + "-thumbnail";
		}

		return newFileName;
	}

	/**
	 * 上传多个文件
	 * @param files
	 * @return 上传文件成功后的结果集
	 * @author wupanhua
	 */
	@Override
	public YKUPResult uploadMultipleFile(List<File> files) {
		return null;
	}

	/**
	 * Description:
	 * <创建一个指定有效期的数据访问链接>
	 * @author wupanhua
	 * @date 11:30 2020-03-03
	 * @param path 存储路径
	 * @param expire 有效时间（s）
	 * @exception Exception 文件创建异常
	 * @return void
	 */
	@Override
	public String crateFileExpireUrl(String path, int expire){
		GetPresignedObjectUrlArgs originArgs = GetPresignedObjectUrlArgs.builder()
				.bucket(minioProperties.getBucketName())
				.method(Method.GET)
				.expiry(expire)
				.object(path).build();
		try {
			return minioClient.getPresignedObjectUrl(originArgs);
		} catch (InsufficientDataException | ErrorResponseException | InternalException |InvalidKeyException |InvalidResponseException | IOException | NoSuchAlgorithmException | XmlParserException | ServerException e) {
			log.error("error info ", e);
		}
		return null;
	}

	/**
	 * Description:根据路径删除文件
	 * <>
	 * @author songcx
	 * @date 14:32 2021/2/2
	 * @param path 1
	 * @return boolean
	 **/
	@Override
	public boolean deleteFile(String path){
		try {
			minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioProperties.getBucketName()).object(path).build());
		} catch (MinioException | GeneralSecurityException | IOException e) {
			log.error("下载失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.SERVER_UNAVAILABLE.getMsg());
		}
		return true;
	}

	/**
	 * 生成一个路径
	 * @param fileName 1
	 * @author wenxiaopeng
	 * @date 2021/07/09 11:24
	 * @return java.lang.String
	 **/
	private String generatePath(String fileName) {
		return DateUtil.format(new Date(), DateUtil.DateStyle.YEARMONTHDAY) + "/" + PathUtil.generatePath(UUIDUtil.get32UUID()) + "/" + fileName;
	}

}
