package example;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

// Handler value: example.Handler
public class ReporterHandler implements RequestHandler<ScheduledEvent, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(ReporterHandler.class);

    @Override
    public String handleRequest(ScheduledEvent cwevent, Context context) {
        try {
            //Accessing DB
            AmazonDynamoDB dbclient = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                            "https://dynamodb.us-west-2.amazonaws.com","us-west-2")).build();

            DynamoDB dynamoDB = new DynamoDB(dbclient);
            Table table = dynamoDB.getTable("MailTest");
            String userid = "meiwen.li@west.cmu.edu";
            GetItemSpec spec = new GetItemSpec().withPrimaryKey("UserId",userid);

            try {
                System.out.println("Attempting to read the item...");
                Item outcome = table.getItem(spec);
                System.out.println("GetItem succeeded: " + outcome);
            } catch (Exception e) {
                System.err.println("Unable to read item: " + userid);
                System.err.println(e.getMessage());
            }
            //new mail
            String region = "us-west-2";

            String FROM = "meiwen.li@west.cmu.edu";
            String TO = "meiwen.li@west.cmu.edu";
            String SUBJECT = "Amazon SES test (AWS SDK for Java)";

            String HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>"
                    + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
                    + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>"
                    + "AWS SDK for Java</a>";
            String TEXTBODY = "This email was sent through Amazon SES "
                    + "using the AWS SDK for Java.";

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

        return "Ok";
    }
}


