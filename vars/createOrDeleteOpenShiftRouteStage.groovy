/**
* Stage: createOrDeleteOpenShiftRouteStage
* Usage: createOrDeleteOpenShiftRouteStage ocServer: "http://xxx", namespace: "cns", cdEnv: "dev", projectName: "app", appAllConfig: appAllConfig
* Parameter: 
*       ocServer (required) (OpenShift Server)
*       namespace (required) (OpenShift Namespace, Default is Product Name)
*       cdEnv (required) (Product Env)
*       projectName (required) (Project Name)
*       appAllConfig (required) (Product All config)
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
    stage("CreateOrUpdate Deployment") {
        def ocAccessToken = cd.getOpenShiftAccessToken(config.cdEnv, config.ocServer)
        def ocCommandPostfix = cd.getOcCommandPostfix(config.ocServer, config.namespace, ocAccessToken)
        def needRoute = config.appAllConfig.needRoute
        def existRoute = cd.existOpenShiftRoute(config.projectName, ocCommandPostfix)
        if (needRoute && !existRoute) {
            cd.createOpenShiftRoute(config.projectName, config.cdEnv, ocCommandPostfix)
        } else if (!needRoute && existRoute) {
            cd.deleteOpenShiftRoute(config.projectName, ocCommandPostfix)
        }
    }
}
