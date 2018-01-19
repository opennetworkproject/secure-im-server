package com.opennetwork.secureim.server.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.opennetwork.secureim.server.configuration.AttachmentsConfiguration;
import com.opennetwork.secureim.server.configuration.AttachmentsConfiguration;

import java.net.URL;
import java.util.Date;

public class UrlSigner {

  private static final long   DURATION = 60 * 60 * 1000;

  private final AWSCredentials credentials;
  private final String bucket;

  public UrlSigner(AttachmentsConfiguration config) {
    this.credentials = new BasicAWSCredentials(config.getAccessKey(), config.getAccessSecret());
    this.bucket      = config.getBucket();
  }

  public URL getPreSignedUrl(long attachmentId, HttpMethod method) {
    AmazonS3                    client  = new AmazonS3Client(credentials);
    GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, String.valueOf(attachmentId), method);
    
    request.setExpiration(new Date(System.currentTimeMillis() + DURATION));
    request.setContentType("application/octet-stream");

    client.setS3ClientOptions(S3ClientOptions.builder().setAccelerateModeEnabled(true).build());

    return client.generatePresignedUrl(request);
  }

}
