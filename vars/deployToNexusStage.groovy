/**
 * Stage: deployToNexusStage
 * Usage: deployToNexusStage nexusUrl: "http://", repoName: "SnapShot", pom:pom, folderName: "xxxx"
 *    Or: deployToNexusStage(["nexusUrl": "http://", "repoName": "SnapShot", "pom": pom, "folderName": "xxxx"])
 * Parameter:
 *       nexusUrl (required) (Nexus Url)
 *       repoName (required) (repo Name)
 *       pom (required) (pom Object)
 *       folderName (required) (file in this folder)
 *
 * @author: wang.j86@foxmail.com
 * @version: 1.0
 * @date: 2018-05-11
 */


import com.forgetting.cicd.CIOperation

def call(Map config = [:]) {
    def ci = new CIOperation(this)
    stage("Upload Jar To Nexus") {
        if (StringUtils.isEmpty(config.nexusUrl) || StringUtils.isEmpty(config.repoName) || pom == null || StringUtils.isEmpty(config.folderName)) {
            error "${config.toString()} parameter is incorrect"
        } else {
            ci.deployToNexus(config.nexusUrl, config.repoName, config.pom, config.folderName)
        }
    }
}