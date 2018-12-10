# JMS Bridge Sample

## Go Build!

To build and run this example, you have to follow the steps below:

### 1. Build the Apache Active MQ Docker container

> It's required to have Docker and the AWS CLI installed and configured on your machine.

Go into the active-mq sub-directory:

``` bash
cd active-mq
```

and build the Docker image for Apache Active MQ 5.15.6.

``` bash
docker build -t active-mq-to-amazon-mq/active-mq .
```

### 2. Tag the Docker image and upload it to Amazon ECR

Now we are creating the Amazon ECR repository to store our Docker image, tag the locally created Apache Active MQ Docker image with the tag 'latest' and pushing it to the ECR repository. Before you can run the following commands, please replace '\<account-id>' and '\<region>' with your values.

``` bash
aws ecr create-repository \
  --repository-name active-mq-to-amazon-mq/active-mq

$(aws ecr get-login --no-include-email --region <region>)

docker tag  active-mq-to-amazon-mq/active-mq <account-id>.dkr.ecr.<region>.amazonaws.com/active-mq-to-amazon-mq/active-mq:latest

docker push <account-id>.dkr.ecr.<region>.amazonaws.com/active-mq-to-amazon-mq/active-mq:latest
```

### 3. Prepare the sample Amazon MQ jms-client (responder/consumer)

Go to the sample project sub-directory:

``` bash
cd ../sample-jms-client
```

and create the Amazon ECR repositories which will host the Docker image:

``` bash
aws ecr create-repository \
  --repository-name active-mq-to-amazon-mq/sample-jms-client
```

### 4. Compile, package, docerize and upload the sample to Amazon ECR

> To be able to run this step, it's required to have Java 8 (or later) and Apache Maven installed!

In this step we are using Apache Maven, to automaticelly achieve to following per sample application:
- compile the Java based sample application
- package the application in a self-contained uber-JAR
- create a Docker image which contains the sample
- upload this image to Amazon ECR, a private Docker repository

Next, run the following command:

``` bash
aws ecr get-login --no-include-email
```

It will return an output like the following, where you have to look up the password for the basic Auth against Amazon ECR (followed by the -p):

``` bash
docker login -u AWS -p eyJwY...1MX0= https://xxxxxxxxxxxx.dkr.ecr.eu-central-1.amazonaws.com
```

Now, we have to provide a few configuration parameter to Maven. This is done, by creating (if not present) or extending the Maven settings.xml configuration file, which is located at ~/.m2/settings.xml. It has to have the following configuration entries:

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
    <profiles>
        <profile>
            <id>default</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <!-- configure the AWS account id for your account -->
                <aws-account-id>xxxxxxxxxxxx</aws-account-id>
            </properties>
        </profile>
    </profiles>

    <servers>
        <!-- Maven is using these configurations for basic Auth to push your image to Amazon ECR -->
        <server>
            <!-- chose the region your are using. I'm using eu-central-1 (Frankfurt) -->
            <id>xxxxxxxxxxxx.dkr.ecr.eu-central-1.amazonaws.com</id>
            <username>AWS</username>
            <!-- The password you were looking up by running 'aws ecr get-login --no-include-email'. This password is temporary and you have to update it once a while -->
            <password>eyJw...zE5NH0=</password>
        </server>
    </servers>
</settings>
```

Now, run the following command to do all the 4 steps we mentioned before:  

``` bash
mvn clean deploy
```

After a successful run, you should see a console output like this:

``` bash
------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 5. Launch the AWS CloudFormation template to deploy the sample

> To be able to run this step, it's required to have the [AWS CLI](https://aws.amazon.com/cli/) installed!

Go to the root directory:

``` bash
cd ..
```

Run the first command to launch the AWS CloudFormation template. The second command will wait, until the AWS CloudFormation stack was launched successfuly and ready to use. Alternatively, you can also open your CloudFormation web console and watch the resource creation process. Before you can run the following commands, please replace '\<MQBrokerUserName>' and '\<MQBrokerPassword>' with your values (for simplicity, we are using the same user and password for both brokers). It takes ~ 15 minutes to complete:

```bash
aws cloudformation create-stack \
    --stack-name active-mq-to-amazon-mq \
    --capabilities CAPABILITY_IAM \
    --parameters ParameterKey=MQBrokerUserName,ParameterValue=<MQBrokerUserName> ParameterKey=MQBrokerPassword,ParameterValue=<MQBrokerPassword> \
    --template-body file://master.yaml

aws cloudformation wait stack-create-complete \
    --stack-name active-mq-to-amazon-mq
```

### 6. Test the sample

Open a new tab and got to your Apache Active MQ broker console (your on-premises broker). You can look-up the IP address from your ECS web console [here](https://console.aws.amazon.com/ecs/home?#/clusters/apache-active-mq-cluster/tasks/details), by selecting the task and look-up the public IP. 

http://\<ECS Task IP\>:8161/

* User: admin
* Password: admin

Click **Send** and provide the following parameter:
* Destination: DEMO.QUEUE1
* Correlation ID: 1
* ReplyTo: DEMO.QUEUE2

and click **Send** again. Klick on **Queue** to refresh the page. You should see something like this:
