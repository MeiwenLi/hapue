package example;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.TextDetection;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//for translation
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;

// Handler value: example.Handler
public class Handler implements RequestHandler<S3Event, String> {
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final Logger logger = LoggerFactory.getLogger(Handler.class);
  private static final float MAX_WIDTH = 100;
  private static final float MAX_HEIGHT = 100;
  private final String JPG_TYPE = (String) "jpg";
  private final String JPG_MIME = (String) "image/jpeg";
  private final String PNG_TYPE = (String) "png";
  private final String PNG_MIME = (String) "image/png";
  @Override
  public String handleRequest(S3Event s3event, Context context) {
    try {
      logger.info("EVENT: " + gson.toJson(s3event));
      S3EventNotificationRecord record = s3event.getRecords().get(0);
      
      String srcBucket = record.getS3().getBucket().getName();
      String srcKey = record.getS3().getObject().getUrlDecodedKey();

      String dstBucket = srcBucket + "-trans";
      String dstKey = srcKey + ".txt";

      // Infer the image type.
      Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(srcKey);
      if (!matcher.matches()) {
          logger.info("Unable to infer image type for key " + srcKey);
          return "";
      }
      String imageType = matcher.group(1);
      if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
          logger.info("Skipping non-image " + srcKey);
          return "";
      }

      AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

      //Create a helloworld file for testing the basic functionality
      StringBuilder builder_old = new StringBuilder();
      builder_old.append("hello world");

      //Call rekognition APIs for extract text from a image
      AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

      DetectTextRequest request = new DetectTextRequest()
              .withImage(new Image().withS3Object(new S3Object().withName(srcKey).withBucket(srcBucket)));

      StringBuilder builder = new StringBuilder();

      try {
        DetectTextResult result = rekognitionClient.detectText(request);

        if (result!=null) builder_old.append("DetectTextResult is not null\n");
        else builder_old.append("DetectTextResult is null\n");

        List<TextDetection> textDetections = result.getTextDetections();

        if (textDetections!=null) builder_old.append("textDetections is not null\n");
        else builder_old.append("textDetections is null\n");

        if (textDetections.isEmpty()) builder_old.append("textDetections is empty\n");
        else builder_old.append("textDetections is NOT empty\n");

        //*********************Test  translation logic

        String REGION = "region";
        AWSCredentialsProvider awsCreds = DefaultAWSCredentialsProviderChain.getInstance();

        AmazonTranslate translate = AmazonTranslateClient.builder()
                  .withCredentials(new AWSStaticCredentialsProvider(awsCreds.getCredentials()))
                  .withRegion(REGION)
                  .build();

        TranslateTextRequest request_T = new TranslateTextRequest()
                  .withText("Hello, world")
                  .withSourceLanguageCode("en")
                  .withTargetLanguageCode("zh");
        TranslateTextResult result_T  = translate.translateText(request_T);

        builder_old.append(result_T.getTranslatedText());
        InputStream im_old = new ByteArrayInputStream(builder_old.toString().getBytes("UTF-8"));
        ObjectMetadata om_old = new ObjectMetadata();
        s3Client.putObject(dstBucket,"helloworld.txt", im_old, om_old);

        //*************************end of translation test

        for (TextDetection text: textDetections) {
            if (text.getType().equals("LINE")) {
              TranslateTextRequest request_N = new TranslateTextRequest()
                      .withText(text.getDetectedText())
                      .withSourceLanguageCode("en")
                      .withTargetLanguageCode("zh");
              TranslateTextResult result_N  = translate.translateText(request_N);
              builder.append(result_N.getTranslatedText());
              builder.append("\n");
            }

        }
      } catch(AmazonRekognitionException e) {
        e.printStackTrace();
      }

      //upload the extracted and translated text to S3 as a file
      InputStream im = new ByteArrayInputStream(builder.toString().getBytes("UTF-8") );
      ObjectMetadata om = new ObjectMetadata();

      try {
        s3Client.putObject(dstBucket, dstKey, im, om);
      }
      catch(AmazonServiceException e)
      {
        logger.error(e.getErrorMessage());
        System.exit(1);
      }
      logger.info("Successfully extracted the text from " + srcBucket + "/"
              + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
      return "Ok";

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}