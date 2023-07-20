
<!-- # Repo-Template
This is an Internal WXSD Template to be used for GitHub Repos moving forward. Follow the following steps: Visit https://github.com/wxsd-sales/readme-template/blob/master/README.md for extended details.
-->



<!--   Step 1) Name your repository: Names will ALWAYS start with "bot - ", "integration -", "macro -", or "supportapp -" 
Examples:"bot- <insert repo name>" 
       "integration - <insert repo name>"
       "macro - <insert repo name>"
       "supportapp - <insert repo name>" 

~3 words, kebab case, use words to indicate what it does. Visit https://github.com/wxsd-sales/readme-template/blob/master/README.md for details  
-->

<!--  Step 2) Add One sentence description to your repository: Copy/Paste from Webex Labs Card sentence.
       Example: "Redirect an Auto Attendant caller to an SMS conversation to alleviate Call Queue Agent responsibilities."
-->

<!--  Step 3) Use following Template to copy/paste your details below -->

# Requet Queueing with CUCM
 Welcome to our WXSD DEMO Repo! <!-- Keep this here --> 

## Table of Contents
<!-- ⛔️ MD-MAGIC-EXAMPLE:START (TOC:collapse=true&collapseText=Click to expand) -->
<details>
<summary>(click to expand)</summary>
    
  * [Setup](#setup)
  * [License](#license)  
  * [Disclaimer](#disclaimer)
  * [Questions](#questions)

</details>
<!-- ⛔️ MD-MAGIC-EXAMPLE:END -->

## Setup

### Prerequisites & Dependencies:

- Mobile Integration with valid client ID and client secret. Please refer [Webex Developer Site](https://developer.webex.com/docs/integrations#registering-your-integration) to see how to register your integration.
- Android Studio 4.0 or above (recommended)
- Webex Android SDK version >=3.4.0
- Android SDK Tools 29 or later
- Android API Level 24 or later
- Java JDK 8
- Kotlin - 1.3.+
- Gradle for dependency management

<!-- GETTING STARTED -->

### Installation Steps:
1.  Download or clone this git project and open it in your android studio IDE
2.  Include all these constants in your gradle.properties file
    ```
    CLIENT_ID=""
    CLIENT_SECRET=""
    SCOPE=""
    REDIRECT_URI=""
    ```

3.  Add the following repository to your top-level `build.gradle` file:

        allprojects {
            repositories {
                maven {
                    url 'https://devhub.cisco.com/artifactory/webexsdk/'
                }
            }
        }

4.  Add the `webex-android-sdk` library as a dependency for your app in the `build.gradle` file:

        dependencies {
            implementation 'com.ciscowebex:androidsdk:3.4.0@aar'
        }
        
5.  Run this project on android emulator or on local device

## License
<!-- MAKE SURE an MIT license is included in your Repository. If another license is needed, verify with management. This is for legal reasons.--> 

<!-- Keep the following statement -->
All contents are licensed under the MIT license. Please see [license](LICENSE) for details.


## Disclaimer
<!-- Keep the following here -->  
 Everything included is for demo and Proof of Concept purposes only. Use of the site is solely at your own risk. This site may contain links to third party content, which we do not warrant, endorse, or assume liability for. These demos are for Cisco Webex usecases, but are not Official Cisco Webex Branded demos.
       
