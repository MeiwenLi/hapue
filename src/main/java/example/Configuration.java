package example;

public class Configuration {
    //bucket setting for uploading image to be translated, recognized, putting result and save adio
    public static final String tans_bucket = "hapuetransbucket";
    public static final String rek_bucket = "hapuerekbucket";
    public static final String text_resultBucket = "textresultbucket";
    public static final String audio_resultBucket = "audioresultbucket";

    //region and identity setting
    public static final String regionTransBucket = "us-west-2";
    public static final String regionRekBucket = "us-west-2";
    public static final String regionTextResultBucket = "us-west-2";
    public static final String regionAudioResultBucket = "us-west-2";

    //Tables and their fields
    public static final String recordTable = "StudyRecord";
    public static final String record_user = "UserId";
    public static final String record_time = "StudyTime";
    public static final String record_cn = "TextCN";
    public static final String record_eng = "TextENG";

    public static final String userTable = "Users";
    public static final String user_id = "UserId";
    public static final String user_pwd = "Password";
}
