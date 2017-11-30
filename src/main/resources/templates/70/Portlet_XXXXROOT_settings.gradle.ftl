<#include "./valuables.ftl">
<#assign createPath = "${createPath_val}/${dashcaseProjectName}/settings.gradle">
include "${dashcaseProjectName}-api", "${dashcaseProjectName}-service"<#if generateWeb == true >, "${dashcaseProjectName}-web"</#if>
