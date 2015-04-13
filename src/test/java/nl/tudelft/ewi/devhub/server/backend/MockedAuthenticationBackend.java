package nl.tudelft.ewi.devhub.server.backend;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import nl.tudelft.ewi.devhub.server.database.controllers.Users;
import nl.tudelft.ewi.devhub.server.database.entities.User;

import javax.persistence.EntityNotFoundException;
import java.util.Map;

@Singleton
public class MockedAuthenticationBackend implements AuthenticationBackend {

	private final Map<String, String> usersMap;
	private final Users users;
	
	@Inject
	public MockedAuthenticationBackend(Users users) {
		this.users = users;
		this.usersMap = Maps.newHashMap();
	}
	
	@Override
	public boolean authenticate(String netId, String password) {
		if (usersMap.containsKey(netId)) {
			String storedPassword = usersMap.get(netId);
			return storedPassword.equals(password);
		}
		return false;
	}
	
	@Transactional
	public MockedAuthenticationBackend addUser(String netId, String password, boolean admin) {
		try {
			users.findByNetId(netId);
		}
		catch (EntityNotFoundException e) {
			User user = new User();
			user.setEmail("no-reply@devhub.ewi.tudelft.nl");
			user.setName(netId);
			user.setNetId(netId);
			user.setAdmin(admin);
			users.persist(user);
		}
		
		usersMap.put(netId, password);
		return this;
	}

}
