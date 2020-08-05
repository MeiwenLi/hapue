package example;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

import java.text.SimpleDateFormat;
import java.util.Date;

// save data into dynamodb
// including user id, english, chinese, and time
public class SaveDataController {

    public String saveData(String userID, StringBuilder engSB, StringBuilder chineseBuilder)  {

        //Get time
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// am/pm
        Date date = new Date();// get the current time
        String StudyTime = sdf.format(date);

        //Dynamodb
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://dynamodb.us-west-2.amazonaws.com", "us-west-2"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);


        Table table = dynamoDB.getTable("StudyRecord");

        String UserId = userID;
        String TextENG = engSB.toString();
        String TextCN = chineseBuilder.toString();


        try {
            System.out.println("Adding a new item...");
            PutItemOutcome outcome = table
                    .putItem(new Item().withPrimaryKey("UserId", UserId, "StudyTime", StudyTime)
                            .withString("TextCN", TextCN)
                            .withString("TextENG", TextENG));
            //.withMap("info", infoMap));

            System.out.println("PutItem succeeded:\n" + outcome.getPutItemResult());

        } catch (Exception e) {
            System.err.println("Unable to add item: " + UserId + " " + TextCN);
            System.err.println(e.getMessage());
        }

        return "ok";
    }

    }


    /*
    public void saveData(String userID, StringBuilder engSB, StringBuilder chineseBuilder){

        //Get time
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        String StudyTime = sdf.format(date);

        //Dynamodb
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://dynamodb.us-west-2.amazonaws.com", "us-west-2"))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);



        Table table = dynamoDB.getTable("StudyRecord");

        String UserId = userID;
        String TextENG = engSB.toString();
        String TextCN = chineseBuilder.toString();



        try {
            System.out.println("Adding a new item...");
            PutItemOutcome outcome = table
                    .putItem(new Item().withPrimaryKey("UserId", UserId, "StudyTime", StudyTime)
                            .withString("TextCN", TextCN)
                            .withString("TextENG", TextENG));
            //.withMap("info", infoMap));

            System.out.println("PutItem succeeded:\n" + outcome.getPutItemResult());

        } catch (Exception e) {
            System.err.println("Unable to add item: " + UserId + " " + TextCN);
            System.err.println(e.getMessage());
        }
    }

     */



