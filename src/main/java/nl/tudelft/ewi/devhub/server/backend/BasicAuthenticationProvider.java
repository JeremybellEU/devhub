package nl.tudelft.ewi.devhub.server.backend;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import nl.tudelft.ewi.devhub.server.database.controllers.Users;
import nl.tudelft.ewi.devhub.server.database.entities.User;

import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;

/**
 * The {@code BasicAuthenticationProvider} checks the given password against the
 * password in the Devhub database, and authorizes the user if the user exists
 * in the database and the passwords match. Note that users should be created in
 * the database first, there is no way yet to register new users or to reset a
 * password.
 * 
 * @author Jan-Willem
 *
 */
@Singleton
public class BasicAuthenticationProvider implements AuthenticationProvider {

	private final Provider<Users> usersProvider;

	@Inject
	public BasicAuthenticationProvider(Provider<Users> usersProvider) {
		this.usersProvider = usersProvider;
	}

	@Override
	public AuthenticationSession authenticate(String username, String password)
			throws InvalidCredentialsException {

		try {
			User user = usersProvider.get().findByNetId(username);

			if (user.isPasswordMatch(password)) {
				return new AbstractAuthenticationSession();
			} else {
				throw new InvalidCredentialsException();
			}

		} catch (EntityNotFoundException e) {
			throw new InvalidCredentialsException();
		}
	}

}
