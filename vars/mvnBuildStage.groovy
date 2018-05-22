/**
* Stage: mvnBuildStage
* Usage1: mvnBuildStage isSkipTest: true, isFailureIgnore: true, compilePara: "", folderName: ""
* Usage2: mvnBuildStage(["isSkipTest": true, "isFailureIgnore": true, "compilePara": "-U", "folderName": "xxxx"]) 
* Parameter: 
*       isSkipTest (required) (test skip is true)
*       isFailureIgnore (required) (test failure ignore is true)
*       compilePara (optional) (Default Value: "-U -e -X")
*       folderName (required) (code folder)
*
* @author: wang.j86@foxmail.com
* @version: 1.0
* @date: 2018-05-10
*/

@Grab(group='org.apache.commons', module='commons-lang3', version='3.7')
import org.apache.commons.lang3.StringUtils
import com.forgetting.cicd.CIOperation

def call(Map config = [:]) {
    def ci = new CIOperation(this)
    stage("Maven Build") {
        if ((config.isSkipTest==null) || (config.isFailureIgnore==null)|| StringUtils.isEmpty(config.folderName)) {
            error "${config.toString()} parameter is incorrect"
        }else{
            if(StringUtils.isEmpty(config.compilePara)){
                ci.mvnBuild(config.isSkipTest, config.isFailureIgnore, "", config.folderName)
            }else{
                ci.mvnBuild(config.isSkipTest, config.isFailureIgnore, config.compilePara, config.folderName)
            }
            
        }
 
    }
}