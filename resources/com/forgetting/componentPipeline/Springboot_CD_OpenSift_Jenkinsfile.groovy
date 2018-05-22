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
    
    def imageFullUrl = "${jenkinsConfig.Jenkins.registryurl.get(registryEnv)}/$productName/${projectName}:${tagName}"
    println imageFullUrl
    //合并后一个应用的所有配置MAP
    def appAllConfig = cu.getAllAppConfig(productConfig, appConfig, cdEnv, "cdEnv")
        
    println appAllConfig
    mailList = appAllConfig.emailList
        
    createOrUpdateOpenShiftDCStage ocServer: "${jenkinsConfig.Jenkins.cloudserverurl.get(cdEnv)}", namespace: namespace, cdEnv: cdEnv, projectName: projectName, yamlTemplateName: "spring-boot-deployment-template.yaml", imageFullUrl: imageFullUrl, appAllConfig: appAllConfig, folderName: "$Constants.JENKINS_FILE_DIR"
    createOrDeleteOpenShiftRouteStage ocServer: "${jenkinsConfig.Jenkins.cloudserverurl.get(cdEnv)}", namespace: namespace, cdEnv: cdEnv, projectName: projectName, appAllConfig: appAllConfig
        
}catch(Exception e){
    echo  "=========CD失败=========="
    throw e 
}finally{
    cu.notifyEmail(mailList)
    cleanWs()
}