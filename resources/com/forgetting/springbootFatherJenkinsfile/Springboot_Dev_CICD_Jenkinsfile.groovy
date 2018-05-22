@Library('forgetting-pipeline-library@master') _
import com.forgetting.cicd.Constants
import com.forgetting.cicd.CommonUtils

def mailList = []

def cu = new CommonUtils(this)
try{
        
    def productName = "mos-core"
        
    //获得产品的产品配置和Jenkins配置
    gitCheckoutStage  repoUrl: "$Constants.PRODUCT_CONFIG_GIT_URL", branch: "master", folderName: "$Constants.PRODUCT_CONFIG_DIR"
    def jenkinsConfig = readYaml file: "${Constants.PRODUCT_CONFIG_DIR}/${productName}/JenkinsConfig.yaml"
    def productConfig = readYaml file: "${Constants.PRODUCT_CONFIG_DIR}/${productName}/ProductConfig.yaml"

    //获得项目源码
    def sourceGitUrl = productConfig.apps.get(env.JOB_BASE_NAME).gitUrl
    gitCheckoutStage  repoUrl: sourceGitUrl, branch: productConfig.default.developBranch, folderName: "$Constants.APP_SOURCE_DIR"

    //获得项目的pom文件
    def pom = readMavenPom file: "${Constants.APP_SOURCE_DIR}/pom.xml"
        
    build job: 'BASIC_JOB_FOLDER/Springboot_CI_Image_Job', parameters: [string(name: 'productName', value: productName), string(name: 'projectName', value: env.JOB_BASE_NAME), string(name: 'gitBranch', value: productConfig.default.developBranch), string(name: 'ciEnv', value: 'dev'), string(name: 'tagName', value: '')]
    build job: 'BASIC_JOB_FOLDER/Springboot_CD_OpenShift_Job', parameters: [string(name: 'productName', value: productName), string(name: 'registryEnv', value: 'dev'), string(name: 'cdEnv', value: 'dev'), string(name: 'projectName', value: env.JOB_BASE_NAME), string(name: 'tagName', value: pom.version), string(name: 'namespace', value: productName)]

}catch(Exception e){
    echo  "=========CI失败=========="
    throw e 
}finally{
    cu.notifyEmail(mailList)
    cleanWs()
}













    