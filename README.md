# Shared Libaray指南


breeze-shared-library 是一套解决CI/CD过程中所出现的问题的Jenkins-shared-library


通过导入到自己的git库后，通过在jenkins的globe config中进行地址的配置即可完成使用


其中配置分为 JenkinsConfig.yaml   其中包括Jenkins中使用的所有的工具的资源信息

ProductConfig.yaml 配置的为整个应用的配置的信息

microserverXXX.yaml 由供应商配置应用的部署信息

上述的三个文件的共同配置构建了一个微服务的完整配置


通过将所有的CI、CD步骤抽象，并将所有的过程基于不同的技术类型进行归类，并在job上完成子job的拆分，可以有效的做到多纬度的抽象
