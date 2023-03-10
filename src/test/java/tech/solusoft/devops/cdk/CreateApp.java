package tech.solusoft.devops.cdk;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.junit.Test;

import io.microlam.aws.devops.StsUtils;
import io.microlam.aws.devops.cdk.AbstractStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.EndpointConfiguration;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.LayerVersionProps;
import software.amazon.awscdk.services.lambda.CfnFunction.SnapStartProperty;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;
import tech.solusoft.devops.Aws;

public class CreateApp {

    static Environment makeEnv(String account, String region) {
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
    
    @Test
    public void createApp() {
    	Aws.configure();
        App app = new App();

        String account_id = StsUtils.getAccountId();
        
        Environment env = Environment.builder()
                .account(account_id)
                .region(Aws.REGION)
                .build();
        
        new AbstractStack(app, "microlam-demo", 
        		StackProps.builder().env(env).build()) {
			
			@Override
			protected void init(Construct scope, String id, StackProps props) {
				
				//Choose selected values
				boolean java = true; // false = native compilation | true = java lambda
				int version = 11; //Java version = 11 or 17 or 19 or 8
				Architecture architecture = Architecture.X86_64; // Architecture.ARM_64 or Architecture.X86_64
				
				//Do not modify this
				String arch = (architecture == Architecture.ARM_64)?"arm64":"amd64";
				
				Bucket bucket = new Bucket(this, Aws.DEPLOYMENT_BUCKET);
				
				//The Java 17/19 layer
				LayerVersion javalayer = null;
				if (java && ((version == 17) || (version == 19))) {
				  javalayer = new LayerVersion(this, "Java" + version + "Layer-"+ arch, LayerVersionProps.builder()
				        .layerVersionName("Java" +  version +"Layer-" + arch)
				        .description("Java "+ version + " " + arch)
				        .compatibleRuntimes(Arrays.asList(software.amazon.awscdk.services.lambda.Runtime.PROVIDED_AL2))
				        .code(Code.fromAsset("target/lambda-java"+ version + "-layer-" + ((version == 17)?"17.0.5.8.1_1":"19.0.1.10.1") + "-"+ arch + ".zip"))
				        .build());
				}
				
			    @SuppressWarnings("serial")
				Function.Builder handlerBuilder = Function.Builder.create(this, "MicrolamDemo")			    		  
		               .functionName("MicrolamDemo")
	               	   .architecture(architecture)
		               .handler("tech.solusoft.lambda.MicrolamDemo")
		               .memorySize(512)
		               .timeout(Duration.seconds(20))
		               .environment(new HashMap<String, String>() {{
		                  put("BUCKET", bucket.getBucketName());
		               }});
			    
			    //Code
			    if (java) {
			    	handlerBuilder.code(Code.fromAsset("target/microlam-demo-1.0-SNAPSHOT-aws-lambda.zip"));
			    }
			    else { //Native
			    	handlerBuilder.code(Code.fromAsset("target/microlam-demo-1.0-SNAPSHOT-aws-lambda-native.zip"));			    	
			    }

			    //Runtime
			    if (java && (version == 8)) {
			    	handlerBuilder.runtime(software.amazon.awscdk.services.lambda.Runtime.JAVA_8_CORRETTO);
			    }
			    else if (java && (version == 11)) {
			    	handlerBuilder.runtime(software.amazon.awscdk.services.lambda.Runtime.JAVA_11); 
			    }
			    else { //version = 17 or 19
			    	handlerBuilder.runtime(software.amazon.awscdk.services.lambda.Runtime.PROVIDED_AL2);
			    }

			    //Java 17/19 layer if necessary
				if (java && ((version == 17) || (version == 19))) {
					handlerBuilder.layers(Collections.singletonList(javalayer));
				}			    
			    
				Function handler =  handlerBuilder.build();
				Alias alias = handler.addAlias("live");
				
		        if (java && (version == 11) && (architecture == Architecture.X86_64)) {
			        // Currently the CDK has not delivered L2 support for SnapStart, need to use L1 support (see https://github.com/aws/aws-cdk/issues/23153)
			        ((CfnFunction) handler.getNode().getDefaultChild()).setSnapStart(SnapStartProperty.builder().applyOn("PublishedVersions").build());
		        }
		       
		        bucket.grantReadWrite(handler);
		        
		        RestApi api = RestApi.Builder.create(this, "microlam-demo-api")
		                .restApiName("microlam-demo").description("MicrolamDemo API.")
		                .endpointConfiguration(EndpointConfiguration.builder().types(Arrays.asList(EndpointType.REGIONAL)).build())
		                .build();
		        
		        LambdaIntegration lambdaIntegration1 = LambdaIntegration.Builder.create(alias)
		        		.proxy(true)
		        		.allowTestInvoke(true)
		        		.build();
		        LambdaIntegration lambdaIntegration2 = LambdaIntegration.Builder.create(alias)
		        		.proxy(true)
		        		.allowTestInvoke(true)
		        		.build();

		        api.getRoot().addResource("sum").addMethod("POST", lambdaIntegration1);
		        api.getRoot().addResource("mult").addMethod("POST", lambdaIntegration2);
			}
		}; 
         
        app.synth();
    }
}
