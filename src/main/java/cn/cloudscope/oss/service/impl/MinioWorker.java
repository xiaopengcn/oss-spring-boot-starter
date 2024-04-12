package cn.cloudscope.oss.service.impl;

import cn.cloudscope.oss.bean.DocumentReturnCodeEnum;
import cn.cloudscope.oss.bean.PreSingUploadParam;
import cn.cloudscope.oss.config.properties.MinioProperties;
import cn.cloudscope.oss.service.StorageWorker;
import cn.cloudscope.oss.utils.FileUtil;
import cn.cloudscope.oss.utils.UUIDUtil;
import com.google.common.collect.Maps;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PostPolicy;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
public class MinioWorker implements StorageWorker {

	private final MinioClient minioClient;

	private final MinioProperties minioProperties;

	public MinioWorker(MinioProperties minioProperties) {

		this.minioProperties = minioProperties;
		this.minioClient = MinioClient.builder().credentials(minioProperties.getAccessKey(),minioProperties.getSecretKey())
				.endpoint(minioProperties.getEndPoint()).build();
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

	@Override
	public String doUpload(InputStream stream, String bucket, String path, String originName) {
		if (null != stream) {
			try {
				HashMap<String, String> header = Maps.newHashMap();
				if (StringUtils.isNotBlank(originName)) {
					header.put("Content-Disposition", "attachment;filename=" + originName);
				}
				log.info("开始上传文件(by stream)，stream size: {}", stream.available());
				PutObjectArgs args = PutObjectArgs.builder()
						.bucket(bucket)
						.contentType(TYPE_CACHE.getOrDefault(FileUtil.getFileSuffix(originName), ContentType.APPLICATION_OCTET_STREAM.getMimeType()))
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
	public String doUpload(File file, String bucket, String path) {
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


	@Override
	public String copyObject(String originPath, String target, boolean isPublic) {
		try {
			String bucket = getBucket(isPublic);
			ObjectWriteResponse response = minioClient.copyObject(
					CopyObjectArgs.builder().bucket(bucket)
							.object(target)
							.source(CopySource.builder().bucket(bucket).object(originPath).build())
							.build()
			);
			if (null != response) {
				return response.object();
			}
			return null;
		} catch (Exception e) {
			log.error("复制失败", e);
			throw new RuntimeException(DocumentReturnCodeEnum.BACKUP_FAILED.getMsg());
		}
	}

	private String doGenerateUrl(String key, int expiresIn) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, XmlParserException, ServerException {
		GetPresignedObjectUrlArgs originArgs = GetPresignedObjectUrlArgs.builder()
				.bucket(minioProperties.getBucketName())
				.method(Method.GET)
				.expiry(expiresIn)
				.object(key).build();
		return minioClient.getPresignedObjectUrl(originArgs);
	}

	@Override
	public String getEndpoint() {
		return minioProperties.getEndPoint();
	}

	@Override
	public String getBucket(boolean isPublic) {
		return isPublic ? minioProperties.getBucketPublic() : minioProperties.getBucketName();
	}

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

	@Override
	public Map<String, String> preSignUpload(PreSingUploadParam param) {
		String bucket = getBucket(param.isPublic());
		String suffix = FileUtil.getFileSuffix(param.getFilename());
		String path = generatePath(UUIDUtil.buildUuid() + "." + suffix);
		// 设置凭证过期时间
		if(null != param.getExpiresIn()) {
			param.setExpiresIn(Duration.ofMinutes(10));
		}
		ZonedDateTime expirationDate = ZonedDateTime.now().plusMinutes(param.getExpiresIn().toMinutes());
		// 创建一个凭证
		PostPolicy policy = new PostPolicy(bucket, expirationDate);
		policy.addEqualsCondition("key", path);
		// 5kiB to 50MiB.
		if(null == param.getSize()) {
			param.setSize(50 * 1024 * 1024);
		}
		policy.addContentLengthRangeCondition(5 * 1024, param.getSize());
		if(null != param.getContentType()) {
			policy.addStartsWithCondition("Content-Type", param.getContentType());
		}
		policy.addEqualsCondition("success_action_status", String.valueOf(200));
		try {
			// 生成凭证并返回
			final Map<String, String> map = minioClient.getPresignedPostFormData(policy);
			map.put("key", path);
			return map;
		} catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
			log.error("get upload error: {}", e.getMessage());
		}

		return null;
	}

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

}
