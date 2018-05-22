/**
* Stage: gitCheckoutStage
* Usage: gitCheckoutStage repoUrl: "xxxxx", branch: "1.0.0", folderName: "xxxx"
*    Or: gitCheckoutStage(["repoUrl": "xxxx", "branch": "1.0.0", "folderName": "xxxx"])
* Parameter: 
*       repoUrl (required) (Git Url)
*       branch (required) (Git Branch)
*       folderName (required) (Git pull files to this folder)
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
    stage("Git Pull ${config.repoUrl}") {
        if (StringUtils.isEmpty(config.repoUrl) || StringUtils.isEmpty(config.branch)|| StringUtils.isEmpty(config.folderName)) {
            error "${config.toString()} parameter is incorrect"
        }else{
            ci.gitCheckout(config.repoUrl, config.branch, config.folderName)
        }
        
    }
}