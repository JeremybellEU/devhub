package nl.tudelft.ewi.devhub.server.backend;

import com.google.common.collect.Lists;
import nl.tudelft.ewi.devhub.server.database.entities.GroupMembership;
import nl.tudelft.ewi.devhub.server.database.entities.User;
import nl.tudelft.ewi.devhub.server.web.errors.ApiError;
import nl.tudelft.ewi.git.client.GitClientException;
import nl.tudelft.ewi.git.client.GitServerClientMock;
import nl.tudelft.ewi.git.client.SshKeys;
import nl.tudelft.ewi.git.models.SshKeyModel;
import nl.tudelft.ewi.git.models.UserModel;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SshKeyBackendTest {

	private static final GitServerClientMock gitClient = new GitServerClientMock();
	
	private final SshKeyBackend backend = new SshKeyBackend(gitClient);
	
	private User user;
	private UserModel userModel;
	
	@Before
	public void beforeTest() {
		user = createUser();
		userModel = gitClient.users().ensureExists(user.getNetId());
	}
	
	@Test(expected=ApiError.class)
	public void testCreateInvalidKeyName() throws ApiError, GitClientException {
		backend.createNewSshKey(user, "keyna me", "ssh-rsa AAAA1242342 ");
	}
	
	@Test(expected=ApiError.class)
	public void testCreateInvalidKey() throws ApiError, GitClientException {
		backend.createNewSshKey(user, "keyname", "ss-rsa AAAA1242342 ");
	}
	
	@Test
	public void testCreate() throws ApiError, GitClientException {
		SshKeyModel model = new SshKeyModel();
		model.setContents("ssh-rsa AAAA1242342");
		model.setName("keyname");
		backend.createNewSshKey(user, model.getName(), model.getContents());
		SshKeys keys = gitClient.users().sshKeys(userModel);
		SshKeyModel actual = keys.retrieve(model.getName());
		assertEquals(model, actual);
		assertThat(userModel.getKeys(), contains(model));
	}
	
	@Test(expected=ApiError.class)
	public void testCreateDuplicateName() throws ApiError, GitClientException {
		SshKeyModel model = new SshKeyModel();
		model.setContents("ssh-rsa AAAA1242342");
		model.setName("keyname");
		backend.createNewSshKey(user, model.getName(), model.getContents());
		backend.createNewSshKey(user, model.getName(), model.getContents());
	}
	
	@Test(expected=ApiError.class)
	public void testCreateDuplicateKey() throws ApiError, GitClientException {
		SshKeyModel model = new SshKeyModel();
		model.setContents("ssh-rsa AAAA1242342");
		model.setName("keyname");
		backend.createNewSshKey(user, model.getName(), model.getContents());
		backend.createNewSshKey(user, model.getName().concat("A"), model.getContents());
	}
	
	@Test
	public void testDeleteSshKey() throws ApiError, GitClientException {
		SshKeyModel model = new SshKeyModel();
		model.setContents("ssh-rsa AAAA1242342");
		model.setName("keyname");
		backend.createNewSshKey(user, model.getName(), model.getContents());
		assertThat(userModel.getKeys(), contains(model));
		backend.deleteSshKey(user, model.getName());
		assertTrue(userModel.getKeys().isEmpty());
	}
	
	@Test
	public void testListEmptyKeys() throws ApiError, GitClientException {
		assertTrue(backend.listKeys(user).isEmpty());
		SshKeyModel model = new SshKeyModel();
		model.setContents("ssh-rsa AAAA1242342");
		model.setName("keyname");
		backend.createNewSshKey(user, model.getName(), model.getContents());
		assertThat(backend.listKeys(user), contains(model));
	}
	
	@Test(expected=ApiError.class)
	public void testDeleteNonExistingSshKey() throws ApiError, GitClientException {
		backend.deleteSshKey(user, "abcd");
	}
	
	private final static Random random = new Random();
	
	protected String randomString() {
		return new BigInteger(130, random).toString(32);
	}

	protected User createUser() {
		User user = new User();
		user.setMemberOf(Lists.<GroupMembership> newArrayList());
		user.setNetId(randomString());
		return user;
	}
	
}
