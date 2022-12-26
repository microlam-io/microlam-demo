package tech.solusoft.devops;

import java.io.File;

import org.junit.Test;

import io.microlam.aws.devops.LambdaUtils;
import io.microlam.aws.devops.S3Utils;

public class UploadAndUpdateLambda {

	@Test
	public void process() {
		Aws.configure();

	 	File file = new File("target/microlam-demo-1.0-SNAPSHOT-aws-lambda.zip");
	 	String bucket = Aws.DEPLOYMENT_BUCKET;;
	 	String s3key = S3Utils.uploadFileToS3(file, bucket, 20); //file.getName();//
	 	
	 	LambdaUtils.updateLambdaCode(new String[] {"MicrolamDemo"}, bucket, s3key);
	}
}
