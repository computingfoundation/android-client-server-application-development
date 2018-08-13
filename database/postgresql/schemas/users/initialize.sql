--
-- Initialization
-- -------------------------------------------------------

BEGIN;

DELETE FROM users.user;

INSERT INTO users.user (id, name, country_code, phone_number, email, first_name,
  last_name, created_at)
  VALUES (49105, 'userone_45', '44', '2228245555', 'userOne45@domain.com',
    'u1fname', 'u1lname', '2016-07-18T08:21:31Z');

INSERT INTO users.user (id, name, passw_hash, country_code, phone_number, email,
  first_name, last_name, created_at)
  VALUES (1482, 'seconduser_37', generate_password_hash('user2Passw'),
    '1', '9991538888', 'secondUser37@domain.com', 'u2fname', 'u2lname',
    '2016-06-22T08:21:31Z');

INSERT INTO users.user (id, name, passw_hash, country_code, phone_number, email,
  first_name, last_name, created_at)
  VALUES (91, 'userThree_22', generate_password_hash('user3Passw'),
    '380', '4449727777', 'uThree22@domain.com', 'u3fname', 'u3lname',
    '2016-04-11T08:21:31Z');

INSERT INTO users.user (id, name, country_code, phone_number, email, first_name,
  last_name, created_at)
  VALUES (8247, 'userNumFour_88', '223', '3330486666', 'numFour88@domain.com',
    'u4fname', 'u4lname', '2016-07-20T08:21:31Z');

INSERT INTO users.user (id, name, country_code, phone_number, email, first_name,
  last_name, created_at)
  VALUES (904393, 'userFive_77', '46', '8887432222', 'fifthUser77@domain.com',
    'u5fname', 'u5lname', '2016-08-09T08:21:31Z');

-- -------------------------------------------------------------------

--userone_45
-- UPDATE users.application_user_id_external_login_service_user_id_pair SET
--   external_login_service_user_id = '2f39bz9gag98g',
--   application_user_id = 49105, external_login_service = 'twitter';

-- --userNumFour_98
-- UPDATE users.application_user_id_external_login_service_user_id_pair SET
--   external_login_service_user_id = 'ci29fmaw97fjw',
--   application_user_id = 8247, external_login_service = 'google';

--userFive_77
-- UPDATE users.application_user_id_external_login_service_user_id_pair SET
--   external_login_service_user_id = 'odl9274jn2f81',
--   application_user_id = 904393, external_login_service = 'facebook';

-- -------------------------------------------------------------------

--userone_45
UPDATE users.user_log SET logged_in_at = '2016-06-05T12:11:43Z', logged_out_at =
  '2016-06-05T12:55:21Z', times_logged_in = 4 WHERE user_id = 49105;

--seconduser_37
UPDATE users.user_log SET logged_in_at = '2016-07-18T07:55:49Z', logged_out_at =
  '2016-07-18T08:21:31Z', times_logged_in = 9 WHERE user_id = 1482;

--userThree_22
UPDATE users.user_log SET logged_in_at = '2016-07-02T15:44:54Z', logged_out_at =
  '2016-07-02T15:48:33Z', times_logged_in = 2 WHERE user_id = 91;
  
--userNumFour_98
UPDATE users.user_log SET logged_in_at = '2016-03-26T22:49:12Z', logged_out_at =
  '2016-03-27T23:07:20Z', times_logged_in = 5 WHERE user_id = 8247;

--userFive_77
UPDATE users.user_log SET logged_in_at = '2016-06-19T05:39:53Z', logged_out_at =
  '2016-06-19T06:13:38Z', times_logged_in = 8 WHERE user_id = 904393;



COMMIT;
