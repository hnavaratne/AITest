# Aitest

AiTest is a JIRA add-on which requires the following prerequisites for successful installation. Also, note that Lumos plugin is also a part of AiTest and both components could only be installed together.
Plugin requirements are directly applicable to AiTest. 

#### Prerequisites
* [`Python 3.5 or above`](https://www.python.org/downloads/)

#### Setting up Lumos:
Once your environment is set up, you need to install the following Python dependencies required by the add-on.
*	flask
*	textacy(http://github.com/chartbeat-labs/textacy.git)
*	textblob
*	flask-restful
*	bs4
*	spacy
*	http://github.com/explosion/spacy-models/releases/download/en_core_web_md-2.0.0/en_core_web_md-2.0.0.tar.gz
*	http://github.com/explosion/spacy-models/releases/download/en_core_web_sm-2.0.0/en_core_web_sm-2.0.0.tar.gz

Use the following command to install the dependencies above:
```python
python -m pip install <dependency_name>
```
Once all the dependencies have been installed, use following commands to install required corpora:
```python
python -m textblob.download_corpora
```
```python
python -m spacy download en
```

# Installing Aitest:
* Build the project and locate the `aitest.jar` file created in the target directory
* Launch `JIRA` click on **Settings** icon and select **Add-ons**
* Click **Manage add-ons** from the left navigational panel
* Click **Upload add-on**
* Browse and Open the Aitest jar file to upload to JIRA as an add-on
* Click **Upload**

