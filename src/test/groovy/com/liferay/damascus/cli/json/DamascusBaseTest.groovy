package com.liferay.damascus.cli.json

import com.beust.jcommander.internal.Lists
import com.beust.jcommander.internal.Maps
import com.liferay.damascus.cli.CreateCommand
import com.liferay.damascus.cli.Damascus
import com.liferay.damascus.cli.common.CommonUtil
import com.liferay.damascus.cli.common.DamascusProps
import com.liferay.damascus.cli.common.JsonUtil
import com.liferay.damascus.cli.common.TemplateUtil
import com.liferay.damascus.cli.common.TemplateUtilTest
import com.liferay.damascus.cli.test.tools.TestUtils
import freemarker.core.Configurable
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.RegexFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.StringUtils
import spock.lang.Specification
import spock.lang.Unroll
import groovy.json.*

class DamascusBaseTest extends Specification {
    static def DS = DamascusProps.DS;
    static def workspaceRootDir = TestUtils.getTempPath() + "damascustest";
    static def workspaceName = "workspace"
    static def workTempDir = "";
    static def createCommand;

    def setup() {
        //Cleanup enviroment
        FileUtils.deleteDirectory(new File(workspaceRootDir));
        TemplateUtil.getInstance().clear();

        //Create Workspace
        CommonUtil.createWorkspace(workspaceRootDir, workspaceName);

        //Execute all tests under modules
        workTempDir = workspaceRootDir + DS + workspaceName + DS + "modules";

        TestUtils.setFinalStatic(CreateCommand.class.getDeclaredField("CREATE_TARGET_PATH"), workTempDir + DS);
        createCommand = new CreateCommand();
    }

    @Unroll("Smoke Test for customValue <#key1>:<#value1> | <#key2><#value2>")
    def "Smoke Test for customValue"() {
        when:
        def paramFilePath = workTempDir + DS + "temp.json"
        def projectName = "Todo"
        def liferayVersion = "70"
        def packageName = "com.liferay.test.foo.bar"
        DamascusBase dmsb = TestUtils.createBaseJsonMock(projectName, liferayVersion, packageName, paramFilePath)

        dmsb.customValue = new HashMap<>();
        dmsb.customValue.put(key1, value1)
        dmsb.customValue.put(key2, value2)
        JsonUtil.writer(paramFilePath, dmsb);

        def baseJsonContents = new File(paramFilePath).text;
        def parsedJson = new JsonSlurper().parseText(baseJsonContents)

        then:
        value1 == parsedJson.get("customValue").get(key1)
        value2 == parsedJson.get("customValue").get(key2)

        cleanup:
        FileUtils.deleteQuietly(new File(paramFilePath))

        where:
        key1      | value1     | key2   | value2
        "keytest" | "valutest" | "key2" | "value2"

    }

    @Unroll("Smoke Test for customValue value convert <#key1>:<#value1> | <#key2><#value2>")
    def "Smoke Test for customValue value convert"() {
        when:
        //
        // Generate custom value base.json
        //
        def paramFilePath = workTempDir + DS + DamascusProps.BASE_JSON
        def projectName = "Todo"
        def expectedProjectDirName = "todo"
        def liferayVersion = "70"
        def packageName = "com.liferay.test.foo.bar"

        // Once clear _cfg to initialize with an actual test target template directory
        TemplateUtil.getInstance().clear()

        DamascusBase dmsb = TestUtils.createBaseJsonMock(projectName, liferayVersion, packageName, paramFilePath, false)

        dmsb.customValue = new HashMap<>();
        dmsb.customValue.put(key1, value1)
        dmsb.customValue.put(key2, value2)
        JsonUtil.writer(paramFilePath, dmsb);

        //
        // Test template
        //
        def targetDir = DamascusProps.TEMPLATE_FILE_PATH;
        def testFileName = "Portlet_testfile.jsp.ftl"
        final FileTreeBuilder tf = new FileTreeBuilder(new File(targetDir))
        tf.dir(DamascusProps.VERSION_70) {
            file(testFileName) {
                withWriter('UTF-8') { writer ->
                    writer.write '''
                        <#include "./valuables.ftl">
                        <#assign createPath = "${entityWebResourcesPath}/testfile.jsp">
                        
                        <#if damascus.customValue?exists>
                        <h2>${damascus.customValue["key1"]}</h2>
                        <h2>${damascus.customValue["key2"]}</h2>
                        </#if>
'''.stripIndent()
                }
            }
        }

        //Run damascus -create
        String[] args = ["-create"]
        Damascus.main(args)

        //Target path map of a project
        def pathMap = TestUtils.getPathMap(expectedProjectDirName)
        def targetFile1 = FileUtils.listFiles(new File(pathMap["webPath"]), new RegexFileFilter(".*testfile.jsp"), TrueFileFilter.INSTANCE)

        then:
        targetFile1.each{
            def m1 = (it.text ==~ /.*FOOFOO.*/)
            def m2 = (it.text ==~ /.*BARBAR.*/)
            assert m1 instanceof Boolean
            assert m2 instanceof Boolean
        }

        cleanup:
        // Delete test file
        FileUtils.deleteQuietly(new File(targetDir + DS + DamascusProps.VERSION_70 + DS + testFileName))

        where:
        key1   | value1   | key2   | value2
        "key1" | "FOOFOO" | "key2" | "BARBAR"
    }
}
