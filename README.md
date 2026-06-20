# RuM

## Premise:

RuM Launcher LLM is a fork based on the following repository https://bitbucket.org/doorless1634/thesis/src/master/
My aim was to overhaul the Declare Editor and his chatbot Declo by integrating LLM functionalities to the system.

To use the Declare Editor as intended, an Api developed by me is required and can be found at the following link https://github.com/Forti-DN/RuM-LLM-API/releases/tag/v1.0.0 , else most of the new functionalities will not work.

### Requirements:

- Java JDK 11

- Minimum resolution: 1280×720

### Notes:

- The current version of RuM can be started from the IDE by running theFirst.RumLauncher.java

- The 2019 version of RuM can be started from the IDE by running theFirst.RumOldLauncher.java

- In order to avoid weka warnings from DeclareRulesDataEnhancer, add an environment variable in your IDE (not a system variable). In Intellij: Run-> Edit Configurations -> Environment variables, add variable: Name: _JAVA_OPTIONS, Value: --add-opens=java.base/java.lang=ALL-UNNAMED.

- Rum.java and RumOld.java can be run directly from IDE with the following VM arguments: --module-path "C:\Program Files\Java\javafx-sdk-11.0.2\lib" --add-modules javafx.controls,javafx.fxml,javafx.web

- Can be run with maven using the following goal: clean javafx:run

- Can be built into a fatjar with maven using the following goal: clean package

- Note that lpsolve55.dll and possibly lpsolve55j.dll are needed for some functionalities (DataAware replayer, AlloyLogGenerator), these dlls should be in the same folder as the fatjar

- Example command to add jars to local repository: mvn deploy:deploy-file -DgroupId="net.sf.javailp" -DartifactId=javailp -Dversion="1.2a" -Durl=file:C:\repos\thesis\local-mvn-repo\ -DrepositoryId=local-mvn-repo -DupdateReleaseInfo=true -Dfile=C:\Tmp\javailp-1.2a.jar

- RuM may fail to start if there are any older versions of Java installed on the same machine (even if they are not on the path). This can be solved by using the following VM argument: -DRumDebug=true -Djava.library.path=. 

### License

Licensed under [GNU GPLv3](LICENSE)
