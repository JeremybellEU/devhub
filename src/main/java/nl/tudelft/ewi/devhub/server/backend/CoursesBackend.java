package nl.tudelft.ewi.devhub.server.backend;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.SneakyThrows;
import nl.tudelft.ewi.devhub.server.database.controllers.CourseAssistants;
import nl.tudelft.ewi.devhub.server.database.controllers.Courses;
import nl.tudelft.ewi.devhub.server.database.controllers.Users;
import nl.tudelft.ewi.devhub.server.database.entities.Course;
import nl.tudelft.ewi.devhub.server.database.entities.CourseAssistant;
import nl.tudelft.ewi.devhub.server.database.entities.User;
import nl.tudelft.ewi.devhub.server.web.errors.UnauthorizedException;
import nl.tudelft.ewi.git.client.GitClientException;
import nl.tudelft.ewi.git.client.GitServerClient;
import nl.tudelft.ewi.git.client.GroupMembers;
import nl.tudelft.ewi.git.models.GroupModel;
import nl.tudelft.ewi.git.models.IdentifiableModel;
import nl.tudelft.ewi.git.models.UserModel;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public class CoursesBackend {

    @Inject
    private Courses courses;

    @Inject
    private Users users;

    @Inject
    private GitServerClient gitServerClient;

    @Inject
    private CourseAssistants courseAssistantsDAO;

    @Inject
    @Named("current.user")
    private User currentUser;

    /**
     * Create a course
     * @param course Course to create
     * @throws GitClientException if an GitClientException occurs
     */
    public void createCourse(Course course) throws GitClientException {
        Preconditions.checkNotNull(course);
        checkAdmin();

        Collection<IdentifiableModel> admins = users.listAdministrators().stream()
            .map(this::retrieveUser)
            .collect(Collectors.toList());

        GroupModel groupModel = new GroupModel();
        groupModel.setName(getGitoliteGroupName(course));
        groupModel.setMembers(admins);
        gitServerClient.groups().ensureExists(groupModel);

        try {
            courses.persist(course);
        }
        catch (Exception e) {
            // On failure, delete the group from the Gitolite config
            gitServerClient.groups().delete(groupModel);
            throw e;
        }
    }

    /**
     * Save changes of an edited course
     * @param course
     */
    public void mergeCourse(Course course) {
        Preconditions.checkNotNull(course);
        checkAdmin();
        courses.merge(course);
    }

    /**
     * Set the CourseAssistants for a Course, and add them to the course group on the Git Server
     * to access the repositories
     * @param course Course
     * @param newAssistants List of users assisting this course
     * @throws GitClientException if an GitClientException occurs
     */
    public void setAssistants(Course course, List<User> newAssistants) throws GitClientException {
        Preconditions.checkNotNull(course);
        Preconditions.checkNotNull(newAssistants);
        checkAdmin();

        List<User> assistantsToAdd = Lists.newArrayList(newAssistants);
        List<CourseAssistant> assistants = course.getCourseAssistants();

        GroupModel group = getGitoliteGroup(course);
        GroupMembers groupMembersApi = gitServerClient.groups().groupMembers(group);
        Collection<IdentifiableModel> groupMembers = groupMembersApi.listAll();

        assistants.stream()
                // Filters the existing assistants that should not be
                // removed, but remove them from the list to be added.
                // Remove returns true if the assistant was in the list,
                // so the lambda returns true if the assistant should be
                // removed.
                .filter((assistant) -> !assistantsToAdd.remove(assistant.getUser()))
                .forEach((assistant) -> {
                    courseAssistantsDAO.delete(assistant);
                    removeAssistantFromGroup(groupMembersApi, groupMembers, assistant.getUser());
                });

        assistantsToAdd.forEach((member) -> {
            CourseAssistant courseAssistant = new CourseAssistant();
            courseAssistant.setCourse(course);
            courseAssistant.setUser(member);
            courseAssistantsDAO.persist(courseAssistant);
            addAssistantToGroup(groupMembersApi, groupMembers, member);
        });
    }

    private void checkAdmin() throws UnauthorizedException {
        if(!currentUser.isAdmin()) {
            throw new UnauthorizedException();
        }
    }

    @SneakyThrows
    private UserModel retrieveUser(User user) {
        return gitServerClient.users().ensureExists(user.getNetId());
    }

    private String getGitoliteGroupName(Course course) {
        return "@" + course.getCode().toLowerCase();
    }

    private GroupModel getGitoliteGroup(Course course) throws GitClientException {
        String groupName = getGitoliteGroupName(course);
        return gitServerClient.groups().retrieve(groupName);
    }

    @SneakyThrows
    private void removeAssistantFromGroup(GroupMembers groupMembersApi, Collection<IdentifiableModel> groupMembers, User user) {
        UserModel userModel = retrieveUser(user);
        if(contains(groupMembers, userModel)) {
            groupMembersApi.removeMember(retrieveUser(user));
        }
    }

    @SneakyThrows
    private void addAssistantToGroup(GroupMembers groupMembersApi, Collection<IdentifiableModel> groupMembers, User user) {
        UserModel userModel = retrieveUser(user);
        if(!contains(groupMembers, userModel)) {
            groupMembersApi.addMember(retrieveUser(user));
        }
    }

    private boolean contains(Collection<IdentifiableModel> groupMembers, IdentifiableModel model) {
        return groupMembers.stream().anyMatch((member) ->
            member.getName().equals(model.getName()));
    }

}
