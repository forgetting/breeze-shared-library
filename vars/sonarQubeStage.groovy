/**
* Stage: sonarQubeStage
* Usage1: sonarQubeStage pom: pom, folderName: "xxxx"
* Usage2: sonarQubeStage(["pom": pom, "folderName": "xxxx"]) 
* Parameter: 
*       pom (required) (pom file)
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
    stage("SonarQube") {
        if (config.pom==null || StringUtils.isEmpty(config.folderName)) {
            error "${config.toString()} parameter is incorrect"
        }else{
            if(StringUtils.isEmpty(config.folderName)){
                ci.sonarQubeAnalysis(config.pom, "")
            }else{
                ci.sonarQubeAnalysis(config.pom, config.folderName)
            }
        }
        
    }
}