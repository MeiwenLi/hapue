package example;
import java.io.InputStream;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.comprehend.model.DominantLanguage;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;

import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageRequest;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageResult;


public class AudioHandler implements RequestHandler<S3Event, String>{
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String CHINESE_VOICE_ID = "Zhiyu";
    private static final String ENGLISH_VOICE_ID = "Joanna";
    private static final Logger logger = LoggerFactory.getLogger(AudioHandler.class);
    @Override
    public String handleRequest(S3Event s3Event, Context context){
        logger.info("EVENT: " + gson.toJson(s3Event));
        S3EventNotificationRecord record = s3Event.getRecords().get(0);
        System.out.println("Before S3!!!!!!!!!!!!!!!");
        String srcBucket = record.getS3().getBucket().getName();
        String srcKey = record.getS3().getObject().getUrlDecodedKey();
        String dstBucket = "happytranslateaudioresultbucket";
        String dstKey = srcKey + ".mp3";
        // create client for each aws S3, Polly, and Comprehend
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        AmazonPolly pollyClient = AmazonPollyClientBuilder.defaultClient();
        AmazonComprehend comprehendClient = AmazonComprehendClientBuilder.defaultClient();

        DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
        DescribeVoicesResult describeVoicesResult = pollyClient.describeVoices(describeVoicesRequest);
        Voice voice = describeVoicesResult.getVoices().get(0);
        // read in text content from the src bucket
        String body = s3Client.getObjectAsString(srcBucket, srcKey);
        System.out.println("After S3!!!!!!!!!!!!!!!");

        // detect dominant language from the text content
        DetectDominantLanguageRequest detectDominantLanguageRequest = new DetectDominantLanguageRequest().withText(body);
        DetectDominantLanguageResult detectDominantLanguageResult = comprehendClient.detectDominantLanguage(detectDominantLanguageRequest);
        DominantLanguage dominantLanguage = detectDominantLanguageResult.getLanguages().get(0);
        String languageCode = dominantLanguage.getLanguageCode();
        String voiceId = voice.getId();
        System.out.println("After COmprehend!!!!!!!!!!!");
        if (languageCode.equalsIgnoreCase("zh")){
            voiceId = CHINESE_VOICE_ID;
        }
        else if(languageCode.equalsIgnoreCase("en")){
            voiceId = ENGLISH_VOICE_ID;
        }
        // from text to audio
        SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest().withText(body).withVoiceId(voiceId).withOutputFormat(OutputFormat.Mp3);
        SynthesizeSpeechResult synthesizeSpeechResult = pollyClient.synthesizeSpeech(synthesizeSpeechRequest);
        InputStream speechStream = synthesizeSpeechResult.getAudioStream();
        ObjectMetadata om = new ObjectMetadata();
        System.out.println("After Polly!!!!!!!!!!!!!!!!!!!!!!");
        // put audio output file into destination bucket
        try {
            s3Client.putObject(dstBucket, dstKey, speechStream, om);
        }
        catch(AmazonServiceException e)
        {
            logger.error(e.getErrorMessage());
            System.exit(1);
        }
        logger.info("Successfully extracted the text from " + srcBucket + "/"
                + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
        logger.info("detected dominant language to be " + languageCode);
        return "Ok";
    }
}