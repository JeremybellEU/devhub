package nl.tudelft.ewi.devhub.server.web.resources;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.servlet.RequestScoped;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.devhub.server.backend.CoursesBackend;
import nl.tudelft.ewi.devhub.server.database.controllers.Courses;
import nl.tudelft.ewi.devhub.server.database.controllers.GroupMemberships;
import nl.tudelft.ewi.devhub.server.database.entities.Course;
import nl.tudelft.ewi.devhub.server.database.entities.Group;
import nl.tudelft.ewi.devhub.server.database.entities.User;
import nl.tudelft.ewi.devhub.server.web.errors.UnauthorizedException;
import nl.tudelft.ewi.devhub.server.web.templating.TemplateEngine;
import nl.tudelft.ewi.git.client.GitClientException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The CoursesResource is used to view, create and edit courses.
 *
 * @author Jan-Willem Gmelig Meyling
 */
@Slf4j
@Path("courses")
@RequestScoped
@Produces(MediaType.TEXT_HTML + Resource.UTF8_CHARSET)
public class CoursesResource extends Resource {

    @Inject
    private Courses courses;

    @Inject
    private GroupMemberships memberships;

    @Inject @Named("current.user")
    private User currentUser;

    @Inject
    private CoursesBackend coursesBackend;

    @Inject
    private TemplateEngine templateEngine;

    /**
     * Get an course overview. This page lists the participating courses, assisting courses
     * and administrating courses.
     * @param request the current HttpServletRequest
     * @return a Response containing the generated page
     * @throws IOException if an I/O error occurs
     */
    @GET
    public Response getCourses(@Context HttpServletRequest request) throws IOException {
        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("user", currentUser);
        parameters.put("courses", courses);
        List<Locale> locales = Collections.list(request.getLocales());
        return display(templateEngine.process("courses.ftl", locales, parameters));
    }

    /**
     * Get an overview for a specific course. For users this redirects to the group page.
     * Administrators and assistants are presented with an overview of groups and assignments.
     * @param request the current HttpServletRequest
     * @param courseCode the course code for the course
     * @return a Response containing the generated page
     * @throws IOException if an I/O error occurs
     */
    @GET
    @Path("{courseCode}")
    public Response getCourse(@Context HttpServletRequest request,
                              @PathParam("courseCode") String courseCode) throws IOException {
        Course course = courses.find(courseCode);

        if(!(currentUser.isAdmin() || currentUser.isAssisting(course))) {
            Group group = memberships.forCourseAndUser(currentUser, course);
            if(group != null) {
                return redirect("/courses/" + course.getCode() + "/groups/" + group.getGroupNumber());
            }
            return redirect("/courses");
        }

        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("user", currentUser);
        parameters.put("course", course);

        List<Locale> locales = Collections.list(request.getLocales());
        return display(templateEngine.process("course-view.ftl", locales, parameters));
    }

    /**
     * Edit a course
     * @param request the current HttpServletRequest
     * @param courseCode the course code for the course
     * @param error an error message
     * @return a Response containing the generated page
     * @throws IOException if an I/O error occurs
     */
    @GET
    @Path("{courseCode}/edit")
    public Response getEditPage(@Context HttpServletRequest request,
                                @PathParam("courseCode") String courseCode,
                                @QueryParam("error") String error) throws IOException {

        if(!currentUser.isAdmin()) {
            throw new UnauthorizedException();
        }

        Course course = courses.find(courseCode);

        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("user", currentUser);
        parameters.put("course", course);

        if(error != null)
            parameters.put("error", error);

        List<Locale> locales = Collections.list(request.getLocales());
        return display(templateEngine.process("course-setup.ftl", locales, parameters));
    }

    /**
     * Submit changes to a course
     * @param request the current HttpServletRequest
     * @param id the course id
     * @param courseName name for the course
     * @param templateRepository template repository url for the course
     * @param minGroupSize min group size for the course
     * @param maxGroupSize max group size for the course
     * @param buildTimeout build timeout for the course
     * @return a Response containing the generated page
     */
    @POST
    @Path("{courseCode}/edit")
    public Response editCourse(@Context HttpServletRequest request,
                               @FormParam("id") Integer id,
                               @FormParam("name") String courseName,
                               @FormParam("template") String templateRepository,
                               @FormParam("min") Integer minGroupSize,
                               @FormParam("max") Integer maxGroupSize,
                               @FormParam("timeout") Integer buildTimeout) {

        if(!currentUser.isAdmin()) {
            throw new UnauthorizedException();
        }

        Course course = courses.find(id);
        course.setName(courseName);
        course.setTemplateRepositoryUrl(templateRepository);
        course.setMinGroupSize(minGroupSize);
        course.setMaxGroupSize(maxGroupSize);
        course.setBuildTimeout(buildTimeout);

        try {
            coursesBackend.mergeCourse(course);
        }
        catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
            if(violations.isEmpty()) {
                return redirect("/courses/" + course.getCode() + "/edit?error=error.course-create-error");
            }
            return redirect("/courses/" + course.getCode() + "/edit?error=" + violations.iterator().next().getMessage());
        }
        catch (Exception e) {
            return redirect("/courses/" + course.getCode() + "/edit?error=error.course-create-error");
        }

        return redirect("/courses");
    }

    /**
     * Set up a new course
     * @param request the current HttpServletRequest
     * @param error an error message
     * @return a Response containing the generated page
     * @throws IOException if an I/O error occurs
     */
    @GET
    @Path("setup")
    public Response getSetupPage(@Context HttpServletRequest request,
                                 @QueryParam("error") String error) throws IOException {

        if(!currentUser.isAdmin()) {
            throw new UnauthorizedException();
        }

        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("user", currentUser);
        parameters.put("courses", courses);

        if(error != null)
            parameters.put("error", error);

        List<Locale> locales = Collections.list(request.getLocales());
        return display(templateEngine.process("course-setup.ftl", locales, parameters));
    }

    /**
     * Submit a course creation
     * @param request the current HttpServletRequest
     * @param courseCode course code for the course
     * @param courseName course name for the course
     * @param templateRepository template repository url for the course
     * @param minGroupSize min group size for the course
     * @param maxGroupSize max group size for the course
     * @param buildTimeout build timeout for the course
     * @return a Response containing the generated page
     */
    @POST
    @Path("setup")
    public Response setupCourse(@Context HttpServletRequest request,
                                @FormParam("code") String courseCode,
                                @FormParam("name") String courseName,
                                @FormParam("template") String templateRepository,
                                @FormParam("min") Integer minGroupSize,
                                @FormParam("max") Integer maxGroupSize,
                                @FormParam("timeout") Integer buildTimeout) throws GitClientException {

        if(!currentUser.isAdmin()) {
            throw new UnauthorizedException();
        }

        Course course = new Course();
        course.setCode(courseCode);
        course.setName(courseName);
        course.setTemplateRepositoryUrl(templateRepository);
        course.setMinGroupSize(minGroupSize);
        course.setMaxGroupSize(maxGroupSize);
        course.setBuildTimeout(buildTimeout);
        course.setStart(new Date());

        try {
            coursesBackend.createCourse(course);
        }
        catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
            if(violations.isEmpty()) {
                return redirect("/courses/setup?error=error.course-create-error");
            }
            return redirect("/courses/setup?error=" + violations.iterator().next().getMessage());
        }
        catch (Exception e) {
            return redirect("/courses/setup?error=error.course-create-error");
        }

        return redirect("/courses");
    }

}
