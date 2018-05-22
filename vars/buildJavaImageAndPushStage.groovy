/**
* Stage: buildJavaImageAndPushStage
* Usage: buildJavaImageAndPushStage registryUrl: "http://", productName: "CNS", envName: "dev", pom: pom, tagName: tagName, dockerfileName: "SpringbootDockerfile", appConfig:appConfig, folderName: "xxxx"
*    Or: buildJavaImageAndPushStage(["registryUrl": "http://", "productName": "CNS", "envName": "dev", "pom": pom, "tagName": "tagName", "dockerfileName": "SpringbootDockerfile", "appConfig": appConfig, "folderName": "xxxx"])
* Parameter: 
*       registryUrl (required) (Docker registry Url)
*       productName (required) (Product Name)
*       envName (required) (Env)
*       pom (required) (pom Object)
*       tagName (not required) (Image Tag Name)
*       dockerfileName (required) (dockerfile Name)
*       appConfig (required) (app Config Object)
*       folderName (required) (file in this folder)
*
* @author: wang.j86@foxmail.com
* @version: 1.0
* @date: 2018-05-11
*/
@Grab(group='org.apache.commons', module='commons-lang3', version='3.7')
import org.apache.commons.lang3.StringUtils
import com.forgetting.cicd.CIOperation

def call(Map config = [:]) {
    def ci = new CIOperation(this)
    stage("Build Image and Push") {
        if (StringUtils.isEmpty(config.registryUrl) || StringUtils.isEmpty(config.productName) || StringUtils.isEmpty(config.envName) || config.pom==null || StringUtils.isEmpty(config.dockerfileName)  || config.appConfig==null || StringUtils.isEmpty(config.folderName)) {
            error "${config.toString()} parameter is incorrect"
        }else{
            ci.buildJavaImageAndPush(config.registryUrl, config.productName, config.envName, config.pom, tagName, config.dockerfileName, config.appConfig, config.folderName)
        }
    }
}