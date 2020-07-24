package example;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Attribute;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//for mail
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

//for DB
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// Handler value: example.Handler
public class ReporterHandler implements RequestHandler<ScheduledEvent, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(ReporterHandler.class);
    String recordTable = "StudyRecord";
    String userTable = "Users";
    String userKey = "UserId"; //the primary key of talbe "Users"
    String recordCol1 = "TextCN";
    String recordCol2 = "TextENG";

    @Override
    public String handleRequest(ScheduledEvent cwevent, Context context) {
        try {
            //Accessing DB
            AmazonDynamoDB dbclient = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                            "https://dynamodb.us-west-2.amazonaws.com", "us-west-2")).build();

            DynamoDB dynamoDB = new DynamoDB(dbclient);
            Table rTable = dynamoDB.getTable(recordTable);
            Table uTable = dynamoDB.getTable(userTable);

            List<String> userList = new ArrayList<>();

            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(userTable)
                    .withProjectionExpression(userKey);

            ScanResult result = dbclient.scan(scanRequest);

            //build userList to contain all userId which will be used to find their daily record and send email then
            for (Map<String, AttributeValue> item : result.getItems()) {

                userList.add(item.get(userKey).getS());
                System.out.println("the users in Table Users are" + userKey + " AND ");
            }


            for (String user : userList) {
                //GetItemSpec spec = new GetItemSpec().withPrimaryKey("UserId",user);
                System.out.println("==========we are in the loop of " + user + "\n");

                Map<String, AttributeValue> expressionAttributeValues =
                        new HashMap<String, AttributeValue>();

                //get today's date for query today's record
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDateTime now = LocalDateTime.now();
                ///TODO: get yesterday's time!!! We would better send record for yesterday!
                String date = dtf.format(now);

                System.out.println("Today is " + date);

                String recordTime = date + "000000"; //get the earliest time for that day for later comparison
                expressionAttributeValues.put(":userId", new AttributeValue().withS(user));

                expressionAttributeValues.put(":time", new AttributeValue().withS(recordTime));

                ScanRequest scanRequestr = new ScanRequest()
                        .withTableName(recordTable)
                        .withFilterExpression("UserId = :userId and StudyTime >= :time")
                        .withProjectionExpression(recordCol1 + "," + recordCol2)
                        .withExpressionAttributeValues(expressionAttributeValues);

                System.out.println("the SCAN request to recordTAble is " + scanRequestr.toString() + "\n");

                ScanResult resultr = dbclient.scan(scanRequestr);

                System.out.println("scan request to table recorder has been completed");

                if (resultr.getScannedCount() >0) {

                    StringBuilder learningRecord_sb = new StringBuilder();

                    for (Map<String, AttributeValue> item : resultr.getItems()) {
                        learningRecord_sb.append(item.get(recordCol1).getS() + " => " + item.get(recordCol2).getS() + "\n");
                        System.out.println("the record of " + recordCol1 + " is " + item.get(recordCol1).getS());
                        System.out.println("the record of " + recordCol2 + " is " + item.get(recordCol2).getS());
                    }

                    //new mail
                    System.out.println("the mail content should be " + learningRecord_sb.toString() + "\n");

                    String region = "us-west-2";

                    String FROM = "meiwen.li@west.cmu.edu";
                    String TO = user;
                    String SUBJECT = "Hapue Learning record" + " for " + date;

                    String HTMLBODY = "<h1>YOUR HAPUE LEARNING RECORD</h1>"
                            + "<p>"
                            + learningRecord_sb.toString();

                    String TEXTBODY = learningRecord_sb.toString();
                    try {
                        AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder
                                .standard().withRegion(Regions.US_WEST_2).build();

                        SendEmailRequest request = new SendEmailRequest()
                                .withDestination(
                                        new Destination().withToAddresses(TO))
                                .withMessage(new Message()
                                        .withBody(new Body()
                                                .withHtml(new Content()
                                                        .withCharset("UTF-8").withData(HTMLBODY))
                                                .withText(new Content()
                                                        .withCharset("UTF-8").withData(TEXTBODY)))
                                        .withSubject(new Content()
                                                .withCharset("UTF-8").withData(SUBJECT)))
                                .withSource(FROM);
                        client.sendEmail(request);
                        System.out.println("Email sent!");
                    } catch (Exception ex) {
                        System.out.println("The email was not sent. Error message: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Reporter was not generated" + ex.getMessage());
        }
        return "Ok";
    }
}


