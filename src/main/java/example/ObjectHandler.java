package example;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// Handler value: example.Handler
public class ObjectHandler implements RequestHandler<S3Event, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(ObjectHandler.class);
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

            //get the srcBucket name and object name
            //String srcBucket = Configuration.rek_bucket;
            //src bucket name should be rek_bucket
            String srcBucket = record.getS3().getBucket().getName();
            String srcKey = record.getS3().getObject().getUrlDecodedKey();

            //get the dstBucket name and result name
            String dstBucket = Configuration.text_resultBucket;
            String dstKey = srcKey + ".txt";
            
            StringBuilder engSB = new StringBuilder();
            StringBuilder chineseBuilder = new StringBuilder();

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

            //Call rekognition APIs for extract text from a image
            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(new Image().withS3Object(new S3Object().withName(srcKey).withBucket(srcBucket)))
                    .withMaxLabels(10).withMinConfidence(75F);

            StringBuilder builder = new StringBuilder();

            try {
                DetectLabelsResult result = rekognitionClient.detectLabels(request);
                List<Label> labels = result.getLabels();

                //String REGION = "us-east-2";
                String REGION = Configuration.regionTransBucket;
                AWSCredentialsProvider awsCreds = DefaultAWSCredentialsProviderChain.getInstance();

                AmazonTranslate translate = AmazonTranslateClient.builder()
                        .withCredentials(new AWSStaticCredentialsProvider(awsCreds.getCredentials()))
                        .withRegion(REGION)
                        .build();


                for (Label label : labels) {
                    List<Instance> instances = label.getInstances();
                    //System.out.println("Instances of " + label.getName());
                    if (instances.isEmpty()) {
                        //neglect labels without instance
                        //so that the results are reduced and more specific to the objects in the image
                        continue;
                    } else {
                        for (Instance instance : instances) {
//                    System.out.println("  Confidence: " + instance.getConfidence().toString());
//                    System.out.println("  Bounding box: " + instance.getBoundingBox().toString());
                            //builder.append(label.getName());
                            //get the label name and translate the results
                            TranslateTextRequest request_N = new TranslateTextRequest()
                                    .withText(label.getName())
                                    .withSourceLanguageCode("en")
                                    .withTargetLanguageCode("zh");
                            TranslateTextResult result_N = translate.translateText(request_N);
                            builder.append(result_N.getTranslatedText());
                            builder.append("\n");
                            engSB.append(label.getName());
                            engSB.append("\n");
                        }
                    }
                }
                //if no labels (with instances) are added to the builder
                if (builder.length() == 0 && labels != null) {
                    Label label = labels.get(0);
                    TranslateTextRequest request_N = new TranslateTextRequest()
                            .withText(label.getName())
                            .withSourceLanguageCode("en")
                            .withTargetLanguageCode("zh");
                    TranslateTextResult result_N = translate.translateText(request_N);
                    builder.append(result_N.getTranslatedText());
                    builder.append("\n");
                    engSB.append(label.getName());
                    engSB.append("\n");
                }
            } catch (AmazonRekognitionException e) {
                e.printStackTrace();
            }
            chineseBuilder = builder;
            //upload the extracted and translated text to S3 as a file
            InputStream im = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
            ObjectMetadata om = new ObjectMetadata();

            try {
                s3Client.putObject(dstBucket, dstKey, im, om);
            } catch (AmazonServiceException e) {
                logger.error(e.getErrorMessage());
                System.exit(1);
            }

            //save to DynamoDB
            //public void saveData(String userID, StringBuilder engSB, StringBuilder chineseBuilder);
            Handler.saveData(srcKey, engSB, chineseBuilder);

            logger.info("Successfully extracted the text from " + srcBucket + "/"
                    + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
            return "Ok";

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}