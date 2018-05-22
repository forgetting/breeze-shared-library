/**
* Stage: moveImageToDiffRegistry
* Usage: moveImageToDiffRegistry sourceRegistryURL: "http://", targetRegistryURL: "http://", productName: "cns", pom: pom
*    Or: moveImageToDiffRegistry(["sourceRegistryURL": "http://", "targetRegistryURL": "http://", "productName": "cns", "pom": pom])
* Parameter: 
*       sourceRegistryURL (required) (Source Docker registry Url)
*       targetRegistryURL (required) (Target Docker registry Url)
*       productName (required) (productName)
*       pom (required) (pom Object)
*
* @author: wang.j86@foxmail.com
* @version: 1.0
* @date: 2018-05-11
*/

import com.forgetting.cicd.CiOperation

def call(Map config = [:]) {
    def ci = new CiOperation(this)
    stage("Move Image To Different Docker Registry") {
        if (StringUtils.isEmpty(config.sourceRegistryURL) || StringUtils.isEmpty(config.targetRegistryURL) || StringUtils.isEmpty(config.productName) || pom ==null) {
            error "${config.toString()} parameter is incorrect"
        }else{
            ci.moveImageToDiffRegistry(config.sourceRegistryURL, config.targetRegistryURL, config.productName, config.pom)
        }
    }
}
