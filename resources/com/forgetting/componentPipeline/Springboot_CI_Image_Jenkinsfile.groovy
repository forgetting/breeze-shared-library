@Library('forgetting-pipeline-library@master') _
import com.forgetting.cicd.Constants
import com.forgetting.cicd.CommonUtils

def mailList = []

def cu = new CommonUtils(this)
try{

    //获得产品的产品配置和Jenkins配置
    gitCheckoutStage  repoUrl: "$Constants.PRODUCT_CONFIG_GIT_URL", branch: "master", folderName: "$Constants.PRODUCT_CONFIG_DIR"
    def jenkinsConfig = readYaml file: "${Constants.PRODUCT_CONFIG_DIR}/${productName}/JenkinsConfig.yaml"
    def productConfig = readYaml file: "${Constants.PRODUCT_CONFIG_DIR}/${productName}/ProductConfig.yaml"

    //获得项目配置
    gitCheckoutStage  repoUrl: "$Constants.APPS_CONFIG_GIT_GROUP/${productName}.git", branch: "master", folderName: "$Constants.APP_CONFIG_DIR"
    def appConfig = readYaml file: "${Constants.APP_CONFIG_DIR}/${projectName}.yaml"
    
    //合并后一个应用的所有配置MAP
    def appAllCiConfig = cu.getAllAppConfig(productConfig, appConfig, ciEnv, "ciEnv")
        
    println appAllCiConfig
    mailList = appAllCiConfig.emailList
        
    //获得项目源码
    def sourceGitUrl = productConfig.apps.get(projectName).gitUrl
    gitCheckoutStage  repoUrl: sourceGitUrl, branch: gitBranch, folderName: "$Constants.APP_SOURCE_DIR"

    //获得项目的pom文件
    def pom = readMavenPom file: "${Constants.APP_SOURCE_DIR}/pom.xml"

    //执行maven编译打包
    mvnBuildStage isSkipTest: appAllCiConfig.isSkipTest, isFailureIgnore: appAllCiConfig.isFailureIgnore, compilePara: appAllCiConfig.compilePara, folderName: "$Constants.APP_SOURCE_DIR"

    //执行Sonarqube代码检查
    if(!appAllCiConfig.sonarIgnore){
            sonarQubeStage pom: pom, folderName: "$Constants.APP_SOURCE_DIR" 
    }

    //上传至Nexus库中
    if(!appAllCiConfig.nexusIgnore){
        deployToNexusStage nexusUrl: jenkinsConfig.Jenkins.nexus, repoName: "snapshots", pom:pom, folderName: "$Constants.APP_SOURCE_DIR"
    }
    //打包成Image并推送到Docker Registry中
    buildJavaImageAndPushStage registryUrl: jenkinsConfig.Jenkins.registryurl.get(ciEnv), productName: "$productName", envName: "$ciEnv", pom: pom, tagName: tagName, dockerfileName: "SpringbootDockerfile", appConfig:appConfig, folderName: "$Constants.APP_SOURCE_DIR"

}catch(Exception e){
    echo  "=========CI失败=========="
    throw e 
}finally{
    cu.notifyEmail(mailList)
    cleanWs()
}



