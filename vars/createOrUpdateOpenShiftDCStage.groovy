/**
* Stage: createOrUpdateOpenShiftDCStage
* Usage: createOrUpdateOpenShiftDCStage ocServer: "http://xxx", namespace: "cns", cdEnv: "dev", projectName: "app", yamlTemplateName: "Springboot-deployment.yaml", imageFullUrl: "http://xxx", appAllConfig: appAllConfig, folderName: "xxx"
* Parameter: 
*       ocServer (required) (OpenShift Server)
*       namespace (required) (OpenShift Namespace, Default is Product Name)
*       cdEnv (required) (Product Env)
*       projectName (required) (Project Name)
*       yamlTemplateName (required) (Yaml File Name)
*       imageFullUrl (required) (Image Name)
*       appAllConfig (required) (App All Config)
*       folderName (required)
*
* @author: wang.j86@foxmail.com
* @version: 1.0
* @date: 2018-05-14
*/

@Grab(group='org.apache.commons', module='commons-lang3', version='3.7')
import org.apache.commons.lang3.StringUtils
import com.forgetting.cicd.CDOperation
import com.forgetting.cicd.CommonUtils


def call(Map config = [:]) {
    def cd = new CDOperation(this)
    def cu = new CommonUtils(this)
    
    stage("CreateOrUpdate Deployment") {
        cu.generateDeployYamlFromTemplate(config.yamlTemplateName, config.projectName,  config.imageFullUrl, config.appAllConfig, config.folderName)
        def ocAccessToken = cd.getOpenShiftAccessToken(config.cdEnv, config.ocServer)
        def ocCommandPostfix = cd.getOcCommandPostfix(config.ocServer, config.namespace, ocAccessToken)
        def existDC =cd.existOpenShiftDC(config.projectName, ocCommandPostfix)
        if (existDC) {
            cd.updateOpenShiftDC(config.projectName, ocCommandPostfix, config.folderName)
        } else {
            cd.createOpenShiftDC(ocCommandPostfix, config.folderName)
            cd.createOpenShiftService(config.projectName, ocCommandPostfix)
        }
    }
}
