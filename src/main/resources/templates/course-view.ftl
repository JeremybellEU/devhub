[#import "macros.ftl" as macros]
[@macros.renderHeader i18n.translate("section.courses") /]
[@macros.renderMenu i18n user /]

[#macro renderBreadcrumb course]
    <ol class="breadcrumb">
        <li><a href="/courses">${ i18n.translate("section.courses") }</a></li>
        <li><a href="/courses/${course.getCode()}">${course.getCode()} - ${course.getName()}</a></li>
    </ol>
[/#macro]

<div class="container">

[#if user.isAdmin() || user.isAssisting(course) ]
    [@renderBreadcrumb course /]

    <div class="row">
        <div class="col-md-8">
            <div class=" panel panel-default">
                <div class="panel-heading">
                    ${i18n.translate("course.control.groups")}
                    <a href="/courses/${course.getCode()}/enroll" class="btn btn-link btn-xs pull-right">
                        <span class="glyphicon glyphicon-plus" aria-hidden="true"></span>
                    </a>
                </div>
                <table class="table panel-body">
                    <thead>
                        <tr>
                            <th>${i18n.translate("course.control.group-name")}</th>
                        </tr>
                    </thead>
                    <tbody>
        [#assign groups=course.getGroups()]
        [#if groups?has_content]
            [#list groups as group]
                            <tr>
                                <td><a href="/courses/${course.getCode()}/groups/${group.getGroupNumber()}">${group.getGroupName()}</a></td>
                            </tr>
            [/#list]
        [#else]
                        <tr>
                            <td colspan="2">${i18n.translate("course.control.no-groups")}</td>
                        </tr>
        [/#if]
                    </tbody>

                </table>
            </div>
        </div>
        <div class="col-md-4">
            <div class=" panel panel-default">
                <div class="panel-heading">
                    ${i18n.translate("course.control.assignments")}
                    <a href="#" class="btn btn-link btn-xs pull-right">
                        <span class="glyphicon glyphicon-plus" aria-hidden="true"></span>
                    </a>
                </div>
                <table class="table panel-body">
                    <thead>
                    <tr>
                        <th>${i18n.translate("course.control.assignment")}</th>
                        <th>${i18n.translate("course.control.due-date")}</th>
                        <th>${i18n.translate("course.control.handed-in")}</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>Assignment 1</td>
                        <td>16th March</td>
                        <td>
                            <div class="progress">
                                <div class="progress-bar" role="progressbar" aria-valuenow="60" aria-valuemin="0" aria-valuemax="100" style="width: 60%;">
                                    60%
                                </div>
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>

            <div class=" panel panel-default">
                <div class="panel-heading">
                ${i18n.translate("course.control.assistants")}
        [#if user.isAdmin() ]
                    <a href="/courses/${course.getCode()}/assistants" class="btn btn-link btn-xs pull-right">
                        <span class="glyphicon glyphicon-cog" aria-hidden="true"></span>
                    </a>
        [/#if]
                </div>
                <table class="table panel-body">
                    <thead>
                    <tr>
                        <th>${i18n.translate("course.control.username")}</th>
                        <th>${i18n.translate("course.control.name")}</th>
                    </tr>
                    </thead>
                    <tbody>
        [#assign assistants=course.getAssistants()]
        [#if assistants?has_content]
            [#list assistants as assistant]
                    <tr>
                        <td>${assistant.getNetId()}</td>
                        <td>
                            ${assistant.getName()}
                            <a href="mailto:${assistant.getEmail()}" class="btn btn-default btn-xs pull-right">
                                <span class="glyphicon glyphicon-envelope" aria-hidden="true"></span>
                            </a>
                        </td>
                    </tr>
            [/#list]
        [#else]
                    <tr>
                        <td colspan="2">${i18n.translate("course.control.no-assistants")}</td>
                    </tr>
        [/#if]
                    </tbody>

                </table>
            </div>

            <div class=" panel panel-default">
                <div class="panel-heading">
                    ${i18n.translate("course.control.details")}
                    <a href="/courses/${course.getCode()}/edit" class="btn btn-link btn-xs pull-right">
                        <span class="glyphicon glyphicon-edit" aria-hidden="true"></span>
                    </a>
                </div>
                <table class="table panel-body">
                    <tbody>
    [#if course.getBuildTimeout()?exists]
                        <tr>
                            <th>${i18n.translate("course.control.build-timeout")}</th>
                            <td>${course.getBuildTimeout()} seconds</td>
                        </tr>
    [/#if]
    [#if course.getMinGroupSize()?exists]
                        <tr>
                            <th>${i18n.translate("course.control.min-group-size")}</th>
                            <td>${course.getMinGroupSize()}</td>
                        </tr>
    [/#if]
    [#if course.getMaxGroupSize()?exists]
                        <tr>
                            <th>${i18n.translate("course.control.max-group-size")}/th>
                            <td>${course.getMaxGroupSize()}</td>
                        </tr>
    [/#if]
    [#if course.getTemplateRepositoryUrl()?exists]
                        <tr>
                            <th>${i18n.translate("course.control.template-repository-ul")}</th>
                            <td><code style="font-size:8px;">${course.getTemplateRepositoryUrl()}</code></td>
                        </tr>
    [/#if]
                    </tbody>
                </table>
            </div>
        </div>

    </div>

[#else]

[/#if]


</div>
[@macros.renderScripts /]
[@macros.renderFooter /]
