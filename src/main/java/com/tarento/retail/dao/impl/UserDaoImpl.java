package com.tarento.retail.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import com.tarento.retail.config.JwtTokenUtil;
import com.tarento.retail.dao.RoleDao;
import com.tarento.retail.dao.UserDao;
import com.tarento.retail.dto.CountryDto;
import com.tarento.retail.dto.MasterRoleDto;
import com.tarento.retail.dto.UserCountryDto;
import com.tarento.retail.dto.UserDto;
import com.tarento.retail.dto.UserMasterRoleCountryOrgDto;
import com.tarento.retail.dto.UserRoleDto;
import com.tarento.retail.model.Action;
import com.tarento.retail.model.Country;
import com.tarento.retail.model.KeyValue;
import com.tarento.retail.model.Role;
import com.tarento.retail.model.SearchRequest;
import com.tarento.retail.model.User;
import com.tarento.retail.model.UserAuthentication;
import com.tarento.retail.model.UserDeviceToken;
import com.tarento.retail.model.UserProfile;
import com.tarento.retail.model.mapper.SqlDataMapper;
import com.tarento.retail.model.mapper.SqlDataMapper.UserProfileMapper;
import com.tarento.retail.model.mapper.SqlDataMapper.UserRoleActionMapper;
import com.tarento.retail.model.mapper.SqlDataMapper.UserRoleMapper;
import com.tarento.retail.util.Constants;
import com.tarento.retail.util.Sql;
import com.tarento.retail.util.Sql.Common;
import com.tarento.retail.util.Sql.NamedUserQueries;
import com.tarento.retail.util.Sql.UserQueries;

@Repository(Constants.USER_DAO)

public class UserDaoImpl implements UserDao {

	public static final Logger LOGGER = LoggerFactory.getLogger(UserDaoImpl.class);

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	RoleDao roleDao;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private BCryptPasswordEncoder bcryptEncoder;

	@Override
	public List<Action> findAllActionsByRoleID(Integer roleID) {
		List<Action> actions = new ArrayList<Action>();
		try {
			actions = jdbcTemplate.query(UserQueries.GET_USER_ACTIONS, new Object[] { roleID },
					new SqlDataMapper().new ActionMapper());
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching all the actions by Role ID " + e);
		}
		return actions;
	}

	@Override
	public User findByUsername(String username) {
		User user = null;
		try {
			user = jdbcTemplate.query(UserQueries.SELECT_USER_ON_USERNAME, new Object[] { username, username },
					new SqlDataMapper().new UserMapper()).get(0);
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching the User by Username : " + e);
		}
		return user;
	}

	@Override
	public UserProfileMapper findOne(Long id, Long orgId) {
		UserProfileMapper mapper = new SqlDataMapper().new UserProfileMapper();
		try {
			jdbcTemplate.query(UserQueries.USER_PROFILE_FETCH + Common.WHERE_CLAUSE
					+ UserQueries.USER_ID_EQUAL_CONDITION + UserQueries.AND_CONDITION + UserQueries.USER_ORG_ID,
					new Object[] { id, orgId }, mapper);
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the User By ID : " + e);
			return null;
		}
		return mapper;
	}

	@Override
	public UserProfileMapper findOneUser(Long id) {
		UserProfileMapper mapper = new SqlDataMapper().new UserProfileMapper();
		try {
			jdbcTemplate.query(UserQueries.GET_USER_BY_ID, new Object[] { id }, mapper);
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the User By ID : " + e);
		}
		return mapper;
	}

	@Override
	public UserAuthentication findOneUserAuthentication(Long id) {
		UserAuthentication user = null;
		try {
			user = jdbcTemplate.query(UserQueries.GET_USER_AUTH_DETAILS, new Object[] { id },
					new SqlDataMapper().new UserAuthenticationMapper()).get(0);
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the Users Auth Details : " + e);
		}
		return user;
	}

	@Override
	public User save(final User user) {
		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					String[] returnValColumn = new String[] { Constants.Parameters.ID };
					PreparedStatement statement = con.prepareStatement(UserQueries.SAVE_USER, returnValColumn);
					statement.setString(1, user.getUsername());
					statement.setString(2, user.getPassword());
					statement.setString(3, user.getEmailId());
					statement.setString(4, user.getPhoneNo());
					statement.setBoolean(5, user.getIsActive());
					statement.setBoolean(6, (user.getIsDeleted() != null) ? user.getIsDeleted() : Boolean.FALSE);
					statement.setString(7, user.getOrgId());
					statement.setString(8, user.getTimeZone());
					statement.setString(9, user.getAvatarUrl());
					return statement;
				}
			}, keyHolder);
			Long id = keyHolder.getKey().longValue();
			user.setId(id);
		} catch (Exception e) {
			LOGGER.error(String.format(Constants.EXCEPTION_METHOD, "save user", e.getMessage()));
			return null;
		}
		return user;
	}

	@Override
	public UserAuthentication save(final UserAuthentication user) {
		UserAuthentication user1 = new UserAuthentication();
		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					String[] returnValColumn = new String[] { "id" };
					PreparedStatement statement = con.prepareStatement(UserQueries.SAVE_USER_AUTHENTICATION,
							returnValColumn);
					statement.setLong(1, user.getUserId());
					statement.setString(2, user.getAuthToken());
					return statement;
				}
			}, keyHolder);
			Long id = keyHolder.getKey().longValue();
			System.out.println(id);
			user1 = this.findOneUserAuthentication(id);

		} catch (Exception e) {
			LOGGER.error("Encountered an exception while saving User Authentication : " + e);
		}
		return user1;
	}

	@Override
	public User update(final User user) {
		try {
			jdbcTemplate.update(UserQueries.UPDATE_USER,
					new Object[] { user.getEmailId(), user.getUsername(), user.getPhoneNo(),
							(user.getIsActive() != null) ? user.getIsActive() : Boolean.TRUE,
							(user.getIsDeleted() != null) ? user.getIsDeleted() : Boolean.FALSE, user.getTimeZone(),
							user.getAvatarUrl(), user.getId() });
		} catch (Exception e) {
			LOGGER.error("Encountered an error while updating User Object : " + e);
			return null;
		}
		return user;
	}

	@Override
	public UserProfileMapper findAll(Boolean active, String keyword, List<Long> roles, String countryCode, Long orgId) {
		List<Object> preparedStatementValues = new ArrayList<>();
		UserProfileMapper mapper = new SqlDataMapper().new UserProfileMapper();
		try {
			String queryToExecute = BuildMyQuery(active, keyword, preparedStatementValues, roles, countryCode, orgId);
			LOGGER.info("Query to fetch is  ::: " + queryToExecute);
			LOGGER.info("Prepared Statement Values passed for Query ::: " + preparedStatementValues.toString());
			jdbcTemplate.query(queryToExecute, preparedStatementValues.toArray(), mapper);

		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the User Profile : " + e);
		}
		return mapper;
	}

	private String BuildMyQuery(Boolean active, String keyword, List preparedStatementValues, List<Long> roles,
			String countryCode, Long orgId) {
		StringBuilder builder = new StringBuilder();
		StringBuilder keywordBuilder = new StringBuilder();
		if (StringUtils.isNotBlank(keyword)) {
			keywordBuilder.append("%" + keyword + "%");
		}

		builder.append(UserQueries.USER_PROFILE_FETCH);
		if (active != null || StringUtils.isNotBlank(keyword) || roles != null || StringUtils.isNotBlank(countryCode)
				|| StringUtils.isNotBlank(orgId.toString())) {
			builder.append(Common.WHERE_CLAUSE);
			Boolean andRequired = false;
			if (active != null) {
				if (active)
					builder.append(UserQueries.TAIL_CONDITIONS_USER_ACTIVE);
				else
					builder.append(UserQueries.TAIL_CONDITIONS_USER_INACTIVE);
				andRequired = true;
			}

			if (StringUtils.isNotBlank(countryCode)) {
				if (andRequired)
					builder.append(Common.AND_CONDITION);
				builder.append(UserQueries.TAIL_CONDITIONS_COUNTRY_EQUALS);
				if (countryCode.equals(Constants.CountryList.SWE.toString()))
					preparedStatementValues.add(Constants.CountryList.SWE.getName());
				else if (countryCode.equals(Constants.CountryList.FIN.toString()))
					preparedStatementValues.add(Constants.CountryList.FIN.getName());
				else if (countryCode.equals(Constants.CountryList.NOR.toString()))
					preparedStatementValues.add(Constants.CountryList.NOR.getName());
				else if (countryCode.equals(Constants.CountryList.IND.toString()))
					preparedStatementValues.add(Constants.CountryList.IND.getName());
			}
			if (StringUtils.isNotBlank(keyword)) {
				if (andRequired)
					builder.append(Common.AND_CONDITION);
				builder.append(Common.OPEN_BRACE + UserQueries.TAIL_CONDITIONS_EMAIL_LIKE + Common.OR_CONDITION
						+ UserQueries.TAIL_CONDITIONS_FIRSTNAME_LIKE + Common.OR_CONDITION
						+ UserQueries.TAIL_CONDITIONS_LASTNAME_LIKE + Common.OR_CONDITION
						+ UserQueries.TAIL_CONDITIONS_COUNTRY_LIKE + Common.CLOSE_BRACE);
				preparedStatementValues.add(keywordBuilder.toString());
				preparedStatementValues.add(keywordBuilder.toString());
				preparedStatementValues.add(keywordBuilder.toString());
				preparedStatementValues.add(keywordBuilder.toString());
				andRequired = true;
			}

			if (roles != null && !roles.isEmpty()) {
				if (andRequired)
					builder.append(Common.AND_CONDITION);
				builder.append(UserQueries.TAIL_CONDITIONS_USER_ROLEIN + getIdQuery(roles));
			}
			// if (StringUtils.isNotBlank(orgId.toString())) {
			// if (andRequired) {
			// preparedStatementValues.add(orgId);
			// builder.append("usrrole.org_id=?");
			// }
			// }

			// builder.append(Common.AND_CONDITION).append(UserQueries.USER_ORG_ID);
			builder.append(UserQueries.USER_ORG_ID);
			preparedStatementValues.add(orgId);
			builder.append(UserQueries.ORDER_BY_USER_ID);
			// builder.append(UserQueries.USER_ORG_ID);
		}

		return builder.toString();
	}

	private static String getIdQuery(final List<Long> idList) {
		final StringBuilder query = new StringBuilder("(");
		if (idList.size() >= 1) {
			query.append(idList.get(0).toString());
			for (int i = 1; i < idList.size(); i++)
				query.append(", " + idList.get(i));
		}
		return query.append(")").toString();
	}

	@Override
	public UserRoleMapper findAllRolesByUser(Long userId, String orgId, String username) {
		UserRoleMapper mapper = new SqlDataMapper().new UserRoleMapper();
		try {
			if (StringUtils.isBlank(orgId) && userId != null) {
				jdbcTemplate.query(UserQueries.GET_ROLES_FOR_USER_BY_ID, new Object[] { userId }, mapper);
			} else if (StringUtils.isNotBlank(orgId) && userId != null) {
				jdbcTemplate.query(UserQueries.GET_ROLES_FOR_USER, new Object[] { userId, orgId }, mapper);
			} else if (StringUtils.isNotBlank(username)) {
				jdbcTemplate.query(UserQueries.GET_ROLES_BY_USERNAME, new Object[] { username }, mapper);
			}
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the Roles for a User : " + e);
		}

		return mapper;
	}

	@Override
	public User findMobile(String phoneNo) {
		User user = null;
		try {
			user = jdbcTemplate.query(UserQueries.GET_USER_BY_PHONE, new Object[] { phoneNo },
					new SqlDataMapper().new UserMapper()).get(0);
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching User by Mobile Number : " + e);
		}
		return user;
	}

	@Override
	public Boolean mapUserToRole(UserRoleDto userRole) {
		if (userRole.getRoles() != null || userRole.getRoleId() != null) {
			try {
				jdbcTemplate.update(UserQueries.REMOVE_USER_ROLE_MAP, new Object[] { userRole.getUserId() });
			} catch (Exception ex) {
				LOGGER.error("Encountered an exception while removing the User Role mapping : " + ex);
			}

			try {
				Boolean updateByRoleId = userRole.getRoleId() != null && userRole.getRoleId().size() > 0;
				List<Role> roleList = userRole.getRoles();
				String query = (userRole.getOrgId() != null && userRole.getOrgId() != 0)
						? UserQueries.MAP_USER_TO_ROLE_WITH_ORG
						: UserQueries.MAP_USER_TO_ROLE;
				jdbcTemplate.batchUpdate(query, new BatchPreparedStatementSetter() {
					@Override
					public void setValues(java.sql.PreparedStatement statement, int i) throws SQLException {
						statement.setLong(1, userRole.getUserId());
						statement.setLong(2, updateByRoleId ? userRole.getRoleId().get(i) : roleList.get(i).getId());
						if (userRole.getOrgId() != null && userRole.getOrgId() != 0) {
							statement.setLong(3, userRole.getOrgId());
						}
					}

					public int getBatchSize() {
						return updateByRoleId ? userRole.getRoleId().size() : roleList.size();
					}
				});
				return true;
			} catch (Exception ex) {
				ex.printStackTrace();
				LOGGER.error("Exception Occured while adding Roles to User : " + ex);
			}
		}
		return false;
	}

	@Override
	public UserProfile saveUserProfile(UserProfile profile) {
		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					String[] returnValColumn = new String[] { Constants.Parameters.ID };
					PreparedStatement statement = con.prepareStatement(UserQueries.INSERT_USER_PROFILE,
							returnValColumn);
					statement.setLong(1, profile.getId());
					statement.setString(2, profile.getFirstName());
					statement.setString(3, profile.getLastName());
					statement.setInt(4, profile.getAge());
					statement.setString(5, profile.getPhoneNo());
					statement.setString(6, profile.getDob());
					statement.setString(7, profile.getGender());
					statement.setString(8, profile.getAvatarUrl());
					if (profile.getStartDate() != null) {
						statement.setDate(9, new java.sql.Date(profile.getStartDate().getTime()));
					} else {
						statement.setDate(9, new java.sql.Date(new Date().getTime()));
					}
					if (profile.getEndDate() != null) {
						statement.setDate(10, new java.sql.Date(profile.getEndDate().getTime()));
					} else {
						statement.setDate(10, new java.sql.Date(new Date().getTime()));
					}
					statement.setString(11, profile.getEmailId());
					statement.setString(12, profile.getCountry());
					if (profile.getRegistrationDate() != null) {
						statement.setDate(13, new java.sql.Date(profile.getRegistrationDate().getTime()));
					} else {
						statement.setDate(13, new java.sql.Date(new Date().getTime()));
					}
					statement.setLong(14, (profile.getCreatedBy() != null) ? profile.getCreatedBy() : 0);
					statement.setDate(15, new java.sql.Date(new Date().getTime()));
					statement.setLong(16, (profile.getUpdatedBy() != null) ? profile.getUpdatedBy() : 0);
					statement.setDate(17, new java.sql.Date(new Date().getTime()));
					statement.setString(18, profile.getEmploymentType());
					return statement;
				}
			}, keyHolder);
		} catch (Exception e) {
			LOGGER.error(String.format(Constants.EXCEPTION_METHOD, "saveUserProfile", e.getMessage()));
			return null;
		}
		return profile;
	}

	@Override
	public UserProfile updateUserProfileImage(UserProfile profile) {
		try {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					String[] returnValColumn = new String[] { "id" };
					PreparedStatement statement = con.prepareStatement(UserQueries.Update_USER_PROFILE_PROFILE_IMAGE,
							returnValColumn);
					statement.setString(1, profile.getAvatarUrl());
					statement.setLong(2, profile.getId());
					return statement;
				}
			}, keyHolder);
		} catch (Exception e) {
			LOGGER.error("Encountered an error while updating User Profile image" + e);
		}
		return profile;
	}

	@Override
	public Long checkUserNameExists(String emailId, String phoneNo) {
		Long userId = 0L;
		try {
			userId = jdbcTemplate.queryForObject(UserQueries.GET_USER_ID, new Object[] { emailId, emailId, phoneNo },
					Long.class);
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while finding the UserName Availability : " + e);
		}
		return userId;
	}

	@Override
	public UserProfileMapper findListOfUsers(List<Long> userIdList) {
		UserProfileMapper mapper = new SqlDataMapper().new UserProfileMapper();
		String query = buildMyQuery(userIdList);
		LOGGER.info("Query to execute for fetching the User Profile : " + query);
		try {
			jdbcTemplate.query(query.toString(), new Object[] {}, mapper);
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the User By ID : " + e);
		}
		return mapper;
	}

	private String buildMyQuery(List<Long> userIdList) {
		StringBuilder builder = new StringBuilder(
				UserQueries.USER_PROFILE_FETCH + Common.WHERE_CLAUSE + UserQueries.USER_ID_IN_CONDITION);
		if (!userIdList.isEmpty()) {
			builder.append("(");
			for (int i = 0; i < userIdList.size(); i++) {
				if (i == 0 && i == userIdList.size() - 1) {
					builder.append(userIdList.get(i));
				} else if (i == userIdList.size() - 1) {
					builder.append(userIdList.get(i));
				} else {
					builder.append(userIdList.get(i) + ",");
				}
			}
			builder.append(")");
		}
		return builder.toString();
	}

	@Override
	public UserProfile updateUserProfile(UserProfile profile) {
		try {
			jdbcTemplate.update(UserQueries.UPDATE_USER_PROFILE,
					new Object[] { profile.getFirstName(), profile.getLastName(), profile.getAge(),
							profile.getPhoneNo(), profile.getDob(), profile.getGender(), profile.getStartDate(),
							profile.getEndDate(), profile.getCountry(), new java.sql.Date(new Date().getTime()), 1L,
							profile.getEmploymentType(), profile.getAvatarUrl(), profile.getId() });
		} catch (Exception e) {
			LOGGER.error("Encountered an error while updating User Profile Object : " + e.getMessage());
			return null;
		}
		return profile;
	}

	@Override
	public Long getNumberOfUsers(Long role, Boolean active) {
		Long numberOfUsers = 0L;
		try {
			if (role != null) {
				numberOfUsers = jdbcTemplate.queryForObject(UserQueries.GET_USER_COUNT_FOR_ROLE, new Object[] { role },
						Long.class);
			} else if (active != null) {
				numberOfUsers = jdbcTemplate.queryForObject(UserQueries.GET_USER_COUNT_ON_ACTIVE_STATUS,
						new Object[] { active }, Long.class);
			} else {
				numberOfUsers = jdbcTemplate.queryForObject(UserQueries.GET_USER_COUNT, Long.class);
			}
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching count of Users : " + e);
		}
		return numberOfUsers;
	}

	@Override
	public Long getNumberOfRoles() {
		Long numberOfRoles = 0L;
		try {
			numberOfRoles = jdbcTemplate.queryForObject(UserQueries.GET_ROLE_COUNT, Long.class);
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching count of Roles : " + e);
		}
		return numberOfRoles;
	}

	@Override
	public List<Country> getCountryList() {
		List<Country> countryList = new ArrayList<>();
		try {
			countryList = jdbcTemplate.query(Common.GET_COUNTRY_LIST, new SqlDataMapper().new CountryMapper());
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching Country List: " + e);
		}
		return countryList;
	}

	@Override
	public List<Country> getCountryListForUser(Long userId) {
		List<Country> countryList = new ArrayList<>();
		try {
			countryList = jdbcTemplate.query(Common.GET_COUNTRY_LIST_FOR_USER, new Object[] { userId },
					new SqlDataMapper().new CountryMapper());
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching Country List: " + e);
		}
		return countryList;
	}

	@Override
	public Boolean mapUserToCountry(UserCountryDto userCountry) {
		try {
			jdbcTemplate.update(UserQueries.REMOVE_USER_COUNTRY_MAP, new Object[] { userCountry.getUserId() });
		} catch (Exception ex) {
			LOGGER.error("Encountered an exception while removing the User Country mapping : " + ex);
		}

		int[] values = null;
		try {
			values = jdbcTemplate.batchUpdate(UserQueries.MAP_USER_TO_COUNTRY, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(java.sql.PreparedStatement statement, int i) throws SQLException {
					Country country = userCountry.getCountries().get(i);
					statement.setLong(1, userCountry.getUserId());
					statement.setLong(2, country.getId());
					statement.setBoolean(3, (country.getIsDefault() != null) ? country.getIsDefault() : Boolean.FALSE);
				}

				public int getBatchSize() {
					return userCountry.getCountries().size();
				}
			});
		} catch (Exception ex) {
			LOGGER.error("Exception Occured while adding Countries to User : " + ex);
		}
		if (values.length > 0) {
			return true;
		}
		return false;
	}

	@Override
	public Boolean invalidateToken(String authToken) {
		try {
			jdbcTemplate.update(UserQueries.REMOVE_USER_DEVICE_TOKEN, new Object[] { authToken });
		} catch (Exception e) {
			LOGGER.error("Encountered an error while removing user device token: " + e.getMessage());
			return false;
		}

		try {
			jdbcTemplate.update(UserQueries.INVALIDATE_TOKEN, new Object[] { authToken });
		} catch (Exception e) {
			LOGGER.error("Encountered an error while invalidating Auth Token : " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public Boolean findUserByToken(String authToken) {
		Long countOfUsers = 0L;
		try {
			countOfUsers = jdbcTemplate.queryForObject(UserQueries.SELECT_USER_BY_TOKEN, new Object[] { authToken },
					Long.class);
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching User by auth token: " + e);
		}
		if (countOfUsers > 0)
			return true;
		return false;
	}

	@Override
	public Boolean checkUserTokenExists(Long userId, String deviceToken) {
		Long available = 0L;
		try {
			available = jdbcTemplate.queryForObject(UserQueries.CHECK_USER_DEVICE_TOKEN,
					new Object[] { userId, deviceToken }, Long.class);
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching User Device by Device token: " + e);
		}
		if (available > 0)
			return true;
		return false;
	}

	@Override
	public Boolean updateUserDeviceToken(Long userId, String deviceToken) {
		try {
			jdbcTemplate.update(UserQueries.UPDATE_USER_DEVICE_TOKEN,
					new Object[] { deviceToken, new Date().getTime(), userId });
		} catch (Exception e) {
			LOGGER.error("Encountered an error while updating User Device Token : " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public Boolean insertUserDeviceToken(Long userId, String deviceToken, String deviceId, Long authTokenRef) {
		try {
			jdbcTemplate.update(UserQueries.INSERT_USER_DEVICE_TOKEN,
					new Object[] { userId, deviceToken, deviceId, new Date().getTime(), authTokenRef });
		} catch (Exception e) {
			LOGGER.error("Encountered an error while inserting new User Device Token : " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public List<UserDeviceToken> getDeviceTokenForUserList(List<Long> userIdList) {
		List<UserDeviceToken> tokenList = new ArrayList<>();
		try {
			List<UserDeviceToken> response = jdbcTemplate.query(
					UserQueries.FETCH_USER_DEVICE_TOKEN + getIdQuery(userIdList),
					new SqlDataMapper().new UserDeviceMapper());
			for (UserDeviceToken tokens : response) {
				if (!jwtTokenUtil.isTokenExpired(tokens.getAuthToken())) {
					tokenList.add(tokens);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching User Device Token Map: " + e);
		}
		return tokenList;
	}

	@Override
	public Long fetchAuthTokenReference(String authToken) {
		authToken = authToken.replace(Constants.TOKEN_PREFIX, "").replace(Constants.TOKEN_PREFIX.toLowerCase(), "");
		Long authTokenRef = 0L;
		try {
			authTokenRef = jdbcTemplate.queryForObject(UserQueries.FETCH_AUTH_TOKEN_REF, new Object[] { authToken },
					Long.class);
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching User Device by Device token: " + e);
		}
		return authTokenRef;
	}

	public List<Action> findAllActionsByRoleIDs(List<Long> roleIDs) {
		String roleId = StringUtils.join(roleIDs, ',');
		List<Action> actions = new ArrayList<Action>();
		try {
			actions = jdbcTemplate.query(UserQueries.GET_USER_ACTIONS.replace("<roleIds>", roleId),
					new SqlDataMapper().new ActionMapper());
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching all the actions by Role ID " + e);
		}
		return actions;
	}

	@Override
	public Boolean saveCountry(CountryDto country) {
		try {
			jdbcTemplate.update(UserQueries.ADD_NEW_COUNTRY, new Object[] { country.getCode(), country.getName(),
					country.getCurrency(), country.getPhoneCode(), country.getLogoUrl(), country.getOrgId() });
		} catch (Exception ex) {
			LOGGER.error("Encountered an exception while adding the country : " + ex);
			return false;
		}
		return true;
	}

	@Override
	public Boolean updateCountry(CountryDto country) {
		try {
			jdbcTemplate.update(UserQueries.UPDATE_COUNTRY, new Object[] { country.getCode(), country.getName(),
					country.getCurrency(), country.getPhoneCode(), country.getId() });
		} catch (Exception ex) {
			LOGGER.error("Encountered an exception while updating the country : " + ex);
			return false;
		}
		return true;
	}

	@Override
	public List<Country> getCountryListForOrg(Long orgId) {
		List<Country> countryList = new ArrayList<>();
		try {
			countryList = jdbcTemplate.query(Common.GET_COUNTRY_LIST_FOR_ORG, new Object[] { orgId },
					new SqlDataMapper().new CountryMapper());
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching Country List: " + e);
		}
		return countryList;
	}

	@Override
	public Boolean checkCountryExistsWithCode(String code, Long orgId) {
		Country country = null;
		try {
			country = jdbcTemplate.query(UserQueries.GET_COUNTRY_BY_CODE, new Object[] { code, orgId },
					new SqlDataMapper().new CountryMapper()).get(0);
			if (country != null && country.getId() != null) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching User by Mobile Number : " + e);
		}
		return false;
	}

	@Override
	public Boolean deleteUserToRole(UserRoleDto userRole) {
		String role = "";
		for (int i = 0; i < userRole.getRoles().size(); i++) {
			if (i == 0) {
				role = role + userRole.getRoles().get(i).getId();
			} else {
				role = role + "," + userRole.getRoles().get(i).getId();
			}
		}
		try {
			jdbcTemplate.update(UserQueries.REMOVE_USER_ROLE_MAP + UserQueries.AND_CONDITION
					+ Sql.Common.BY_ROLE_ID.replace("<ROLE_ID>", role), new Object[] { userRole.getUserId() });
		} catch (Exception ex) {
			LOGGER.error("Encountered an exception while removing the User Role mapping : " + ex);
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	@Override
	public Boolean deleteCountryForOrg(CountryDto country) { // my
		try {
			jdbcTemplate.update(Common.DELETE_COUNTRY_FOR_ORG, new Object[] { country.getId(), country.getOrgId() });
		} catch (Exception ex) {
			LOGGER.error("Encounter an exception white deleting the country: " + ex);
			return Boolean.FALSE;
		}
		return Boolean.FALSE;
	}

	@Override
	public Boolean deleteUser(UserDto user) {
		try {
			jdbcTemplate.update(Sql.UserQueries.DELETE_COUNTRY_USER, new Object[] { user.getId() });
			jdbcTemplate.update(Sql.UserQueries.DELETE_USER_ROLE, new Object[] { user.getId() });
			jdbcTemplate.update(Sql.UserQueries.DELETE_USER_PROFILE, new Object[] { user.getId() });
			jdbcTemplate.update(Sql.UserQueries.DELETE_USER, new Object[] { user.getId() });
		} catch (Exception ex) {
			LOGGER.error("Encounter an exception while deleting the user: " + ex);
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	@Override
	public List<UserDto> getUsersByMasterRole(String roleCode, Long orgId) {
		List<UserDto> userList = new ArrayList<UserDto>();
		try {
			userList = jdbcTemplate.query(UserQueries.GET_USERS_BY_MASTER_ROLE, new Object[] { roleCode, orgId },
					new SqlDataMapper().new UserMasterRoleMapper());
		} catch (Exception ex) {
			LOGGER.error("Encounter an exception while getting users which have master role access");
		}
		return userList;
	}

	@Override
	public Boolean mapUserMasterRoleCountryOrg(UserMasterRoleCountryOrgDto userMasterRoleCountryOrg) {
		try {
			jdbcTemplate.update(Sql.UserQueries.MAP_USER_MASTER_ROLE_COUNTRY_ORG,
					new Object[] { userMasterRoleCountryOrg.getMasterRoleId(), userMasterRoleCountryOrg.getCountryId(),
							userMasterRoleCountryOrg.getUserId(), userMasterRoleCountryOrg.getOrgId() });
		} catch (Exception ex) {
			LOGGER.error("Encounter an exception while mapping the user master_role country org : " + ex);
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	@Override
	public List<MasterRoleDto> getMasterRoleByOrgDomainId(Long org_domain_id) {
		List<MasterRoleDto> masterRoleList = new ArrayList<MasterRoleDto>();

		try {
			masterRoleList = jdbcTemplate.query(UserQueries.GET_MASTER_ROLE_LIST_BY_ORG_DOMAIN,
					new Object[] { org_domain_id }, new SqlDataMapper().new MasterRoleMapper());
		} catch (Exception ex) {
			LOGGER.error("Encounter an exception while getting master role list");
		}
		return masterRoleList;
	}

	@Override
	public UserRoleActionMapper findUserRolesActions(String username) {
		UserRoleActionMapper mapper = new SqlDataMapper().new UserRoleActionMapper();
		try {
			jdbcTemplate.query(UserQueries.GET_USER_ROLE_ACTIONS, new Object[] { username }, mapper);
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the User By UserName : " + e);
		}
		return mapper;
	}

	@Override
	public User findOnlyUser(String username) {
		User user = null;
		try {
			user = jdbcTemplate.query(UserQueries.SELECT_ONLY_USER, new Object[] { username, username },
					new SqlDataMapper().new SimpleUserMapper()).get(0);
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching the User by Username : " + e);
		}
		return user;
	}

	@Override
	public UserProfile getUserProfile(String username) {
		try {
			List<UserProfile> userList = jdbcTemplate.query(UserQueries.GET_USER_PROFILE,
					new Object[] { username, username }, new BeanPropertyRowMapper<>(UserProfile.class));
			if (userList.size() != 0) {
				return userList.get(0);
			}
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching the User by Username : " + e);
		}
		return null;
	}

	@Override
	public UserProfileMapper findAll(SearchRequest searchRequest) {
		UserProfileMapper mapper = new SqlDataMapper().new UserProfileMapper();
		try {
			Map<String, Object> paramMap = new HashMap<>();
			String queryToExecute = BuildMyQuery(searchRequest, paramMap);
			System.out.println(queryToExecute);
			System.out.println(paramMap);
			namedParameterJdbcTemplate.query(queryToExecute, paramMap, mapper);
		} catch (Exception e) {
			LOGGER.error("Encountered an exception while fetching the User Profile : " + e);
		}
		return mapper;
	}

	/**
	 * Dynamic query builder
	 * 
	 * @param searchRequest
	 *            SearchRequest
	 * @param paramMap
	 *            Map<String, Object>
	 * @return String
	 */
	private String BuildMyQuery(SearchRequest searchRequest, Map<String, Object> paramMap) {
		StringBuilder builder = new StringBuilder(UserQueries.USER_PROFILE_FETCH);

		Boolean condition = Boolean.FALSE;
		// orgId
		if (searchRequest.getOrgId() != null && searchRequest.getOrgId() > 0) {
			condition = addQueryCondition(builder, condition);
			builder.append(NamedUserQueries.USER_ORG_ID);
			paramMap.put(Constants.Parameters.ORG_ID, searchRequest.getOrgId());
		}
		// active
		if (searchRequest.getActive() != null) {
			condition = addQueryCondition(builder, condition);
			if (searchRequest.getActive()) {
				builder.append(UserQueries.TAIL_CONDITIONS_USER_ACTIVE);
			} else {
				builder.append(UserQueries.TAIL_CONDITIONS_USER_INACTIVE);
			}
			paramMap.put(Constants.Parameters.ACTIVE, searchRequest.getActive());
		}
		// roleId
		if (searchRequest.getRoleId() != null && searchRequest.getRoleId().size() > 0) {
			condition = addQueryCondition(builder, condition);
			builder.append(NamedUserQueries.TAIL_CONDITIONS_USER_ROLEIN);
			paramMap.put(Constants.Parameters.ROLE_ID, searchRequest.getRoleId());
		}
		// keyword search
		if (StringUtils.isNotBlank(searchRequest.getKeyword())) {
			condition = addQueryCondition(builder, condition);
			String keyword = "%" + searchRequest.getKeyword() + "%";
			builder.append(Common.OPEN_BRACE + NamedUserQueries.TAIL_CONDITIONS_EMAIL_LIKE + Common.OR_CONDITION
					+ NamedUserQueries.TAIL_CONDITIONS_FIRSTNAME_LIKE + Common.OR_CONDITION
					+ NamedUserQueries.TAIL_CONDITIONS_LASTNAME_LIKE + Common.OR_CONDITION
					+ NamedUserQueries.TAIL_CONDITIONS_COUNTRY_LIKE + Common.CLOSE_BRACE);
			paramMap.put(Constants.Parameters.EMAIL_ID, keyword);
			paramMap.put(Constants.Parameters.FIRST_NAME, keyword);
			paramMap.put(Constants.Parameters.LAST_NAME, keyword);
			paramMap.put(Constants.Parameters.COUNTRY, keyword);
		}
		// dynamic key value search
		if (searchRequest.getSearch() != null && searchRequest.getSearch().size() > 0) {
			for (Map.Entry<String, Object> entry : searchRequest.getSearch().entrySet()) {
				condition = addQueryCondition(builder, condition);
				if (entry.getValue() instanceof List) {
					if (Constants.UserSearchFields.MAPPING.containsKey(entry.getKey())) {
						builder.append(Constants.UserSearchFields.MAPPING.get(entry.getKey()));
					} else {
						builder.append(entry.getKey());
					}
					builder.append(NamedUserQueries.IN_CLAUSE);
					paramMap.put(Constants.Parameters.IN_VALUE, entry.getValue());
				} else {
					if (Constants.UserSearchFields.MAPPING.containsKey(entry.getKey())) {
						builder.append(Constants.UserSearchFields.MAPPING.get(entry.getKey()));
					} else {
						builder.append(entry.getKey());
					}
					builder.append(NamedUserQueries.APPEND_VALUE);
					paramMap.put(Constants.Parameters.VALUE, entry.getValue());
				}
			}
		}
		// limit & offset
		if (searchRequest.getLimit() > 0) {
			builder.append(NamedUserQueries.LIMIT);
			paramMap.put(Constants.Parameters.LIMIT, searchRequest.getLimit());
		}
		if (searchRequest.getOffset() > 0) {
			builder.append(NamedUserQueries.OFFSET);
			paramMap.put(Constants.Parameters.OFFSET, searchRequest.getOffset());
		}
		return builder.toString();
	}

	/**
	 * Appends Where & and condition to the query builder
	 * 
	 * @param builder
	 *            StringBuilder
	 * @param condition
	 *            Boolean
	 */
	public Boolean addQueryCondition(StringBuilder builder, Boolean condition) {
		if (!condition) {
			builder.append(Common.WHERE_CLAUSE);
		} else {
			builder.append(Common.AND_CONDITION);
		}
		return Boolean.TRUE;
	}

	@Override
	public List<KeyValue> getNumberOfUsersAndRoles() {
		List<KeyValue> userList = new ArrayList<>();
		try {
			userList = jdbcTemplate.query(UserQueries.GET_NUMBER_USER_ROLES,
					new SqlDataMapper().new UserRoleCountMapper());
		} catch (Exception e) {
			LOGGER.error("Encountered an Exception while fetching the User by Username : " + e);
		}
		return userList;
	}

	@Override
	public Boolean setUserPin(String encryptedPin, Long userId) {
		try {
			jdbcTemplate.update(UserQueries.SET_USER_PIN, new Object[] { encryptedPin, userId });
			return Boolean.TRUE;
		} catch (Exception e) {
			LOGGER.error(String.format(Constants.EXCEPTION_METHOD, "setUserPin", e.getMessage()));
			return Boolean.FALSE;
		}
	}

	@Override
	public Boolean validateUserPin(int pin, String username) {
		try {
			List<String> userPin = jdbcTemplate.queryForList(UserQueries.GET_USER_PIN, new Object[] { username },
					String.class);
			if (userPin != null && userPin.size() > 0 && bcryptEncoder.matches(String.valueOf(pin), userPin.get(0))) {
				return Boolean.TRUE;
			}
		} catch (Exception e) {
			LOGGER.error(String.format(Constants.EXCEPTION_METHOD, "validateUserPin", e.getMessage()));
		}
		return Boolean.FALSE;
	}

	@Override
	public Boolean deleteDeviceToken(Long userId, String deviceId) {
		try {
			jdbcTemplate.update(UserQueries.DELETE_DEVICE_TOKEN, new Object[] { userId, deviceId });
			return Boolean.TRUE;
		} catch (Exception e) {
			LOGGER.error(String.format(Constants.EXCEPTION_METHOD, "deleteDeviceToken", e.getMessage()));
		}
		return Boolean.FALSE;
	}

	@Override
	public Boolean updateDeviceAuthRef(Long userId, String deviceToken, Long authId) {
		try {
			jdbcTemplate.update(UserQueries.UPDATE_DEVICE_AUTH_REF, new Object[] { authId, userId, deviceToken });
			return Boolean.TRUE;
		} catch (Exception e) {
			LOGGER.error(String.format(Constants.EXCEPTION_METHOD, "updateDeviceAuthRef", e.getMessage()));
		}
		return Boolean.FALSE;
	}
}
