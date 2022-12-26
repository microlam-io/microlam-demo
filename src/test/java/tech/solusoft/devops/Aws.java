package tech.solusoft.devops;

import io.microlam.aws.auth.AwsProfileRegionClientConfigurator;
import software.amazon.awssdk.regions.Region;

public class Aws {
	
	public final static String PROFILE = "microlam";
	public final static String REGION = "eu-west-1";
	public final static String DEPLOYMENT_BUCKET = "microlam-deploy";
	
	public static void configure() {
	   	AwsProfileRegionClientConfigurator.setProfile(PROFILE);
	 	AwsProfileRegionClientConfigurator.setRegion(Region.of(REGION));
	}

}
