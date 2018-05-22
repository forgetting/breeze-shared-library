@Library('forgetting-pipeline-library@master') _
import com.forgetting.cicd.Constants
import com.forgetting.cicd.CommonUtils

def mailList = []

def cu = new CommonUtils(this)
try{
        
    def productName = "mos-core"
    def namespace = "demo"
    //获得产品的产品配置和Jenkins配置
    gitCheckoutStage  repoUrl: "$Constants.PRODUCT_CONFIG_GIT_URL", branch: "master", folderName: "$Constants.PRODUCT_CONFIG_DIR"
    def jenkinsConfig = readYaml file: "${Constants.PRODUCT_CONFIG_DIR}/${productName}/JenkinsConfig.yaml"
    def productConfig = readYaml file: "${Constants.PRODUCT_CONFIG_DIR}/${productName}/ProductConfig.yaml"

    if(If_Execute_CI){
        build job: 'BASIC_JOB_FOLDER/Springboot_CI_Image_Job', parameters: [string(name: 'productName', value: productName), string(name: 'projectName', value: env.JOB_BASE_NAME), string(name: 'gitBranch', value: tagName), string(name: 'ciEnv', value: 'test'), string(name: 'tagName', value: tagName)]
    }
    if(If_Execute_CD){
        build job: 'BASIC_JOB_FOLDER/Springboot_CD_OpenShift_Job', parameters: [string(name: 'productName', value: productName), string(name: 'registryEnv', value: 'test'), string(name: 'cdEnv', value: CD_Execute_Env), string(name: 'projectName', value: env.JOB_BASE_NAME), string(name: 'tagName', value: tagName), string(name: 'namespace', value: namespace)]
    }
}catch(Exception e){
    echo  "=========CI失败=========="
    throw e 
}finally{
    cu.notifyEmail(mailList)
    cleanWs()
}

