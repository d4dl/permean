--
-- Table structure for table 'user'
--
--Conventions:  id's are <tablename>_id.  
--              Foreign key columns are the same name as the column they key on.
--              This makes sql joins much easier to build.
--              Relationship mapping tables are <table1>_<table2>_rel


CREATE TABLE user (
  user_id BIGINT auto_increment PRIMARY KEY,
  email varchar(255) UNIQUE,
  username varchar(255) NOT NULL UNIQUE,
  password varchar(50) NOT NULL default '',
  seclev int(10) NOT NULL default '1',
  receiveReminders enum('yes','no') NOT NULL default 'yes',
  status enum('unconfirmed','disabled','enabled') NOT NULL default 'unconfirmed',
  createdDate datetime
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- Defines external sources that can access the system 
-- such as facebook or gpsmission.
CREATE TABLE external_source (
  external_source_id BIGINT auto_increment PRIMARY KEY,
  name varchar(64),
  description varchar(255) default '',
  createdDate datetime
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- Defines users from external sources like facebook.
-- and links them to a gridnet user id.
CREATE TABLE external_user (
  external_user_id varchar(64),
  external_source_id BIGINT,
  user_id BIGINT,
  createdDate datetime,
  FOREIGN KEY (external_source_id) REFERENCES external_source (external_source_id),
  FOREIGN KEY (user_id) REFERENCES user (user_id),
  PRIMARY KEY(external_user_id, external_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- The permission table is a record of every assigned permission
-- to users in the database. 
--
CREATE TABLE permission (
  permission_id BIGINT auto_increment PRIMARY KEY,
  domain varchar(35) NOT NULL default '',
  label varchar(35) NOT NULL default '',
  description varchar(255) default '',
  lastModified timestamp NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

ALTER TABLE permission ADD CONSTRAINT UNIQUE (domain,label);


--many to many users' permissions and whether it is granted or denied.
CREATE TABLE user_permission_rel (
  permission_rel_id BIGINT auto_increment PRIMARY KEY,
  permission_id BIGINT,
  user_id BIGINT,
  value int(1) default NULL,
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


--relates users to other users to determine what permissions
--one user gives to another such as watching them and whatnot
CREATE TABLE user_rel (
  user_rel_id BIGINT auto_increment PRIMARY KEY,
  creator_user_id BIGINT,
  referent_user_id BIGINT,
  permission_id BIGINT,
  FOREIGN KEY (permission_id) REFERENCES user_permission_rel (permission_rel_id),
  FOREIGN KEY (creator_user_id) REFERENCES user (user_id),
  FOREIGN KEY (referent_user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE login_token (
  user_id BIGINT NOT NULL,
  token varchar(255) NOT NULL default '',
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
--
--
CREATE TABLE profile (
  profileId BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  firstName varchar(50) default '',
  lastName varchar(50) default '',
  title varchar(35) default '',
  im_type varchar(20) default '',
  im_userid varchar(100) default '',
  phone1 varchar(20) default NULL,
  phone1Type enum('home','mobile','pager','work') default NULL,
  phone2Type enum('home','mobile','pager','work') default NULL,
  phone2 varchar(20) default NULL,
  phone3Type enum('home','mobile','pager','work') default NULL,
  phone3 varchar(20) default NULL,
  lastModified timestamp default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  biography text,
  startDate date default NULL,
  photoUrl varchar(100) default NULL,
  photoWidth int(10) default NULL,
  photoHeight int(10) default NULL,
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

INSERT INTO permission VALUES (1,'php_users','list_users','Allows a user to view lists of users\'s',NOW());
INSERT INTO permission VALUES (2,'php_users','edit_self','Allows a user to edit their profile information',NOW());
INSERT INTO permission VALUES (3,'php_users','edit_users','Allows a user to edit other user information',NOW());
INSERT INTO permission VALUES (4,'php_users','set_perm','Allows a user to add permissions to other users\'s',NOW());
INSERT INTO permission VALUES (5,'php_users','edit_perm','Allows a user to change a users\'s permissions',NOW());
INSERT INTO permission VALUES (6,'php_users','change_other_pass','Allows a user to change other user\s passwords',NOW());

INSERT INTO permission VALUES (7,'global','super_admin','Allows a user to do pretty much anything',NOW());
INSERT INTO permission VALUES (8,'global','goddess','Allows a user create new functionality.',NOW());
INSERT INTO permission VALUES (9,'global','architect','These people have some vested interest in the success of the endeavor.',NOW());
INSERT INTO permission VALUES (10,'global','steward','These guys registered stewardship over a field.',NOW());

INSERT INTO user VALUES (1,'root@localhost','root',password('Quinn4'),9999,'no','enabled',NOW());
INSERT INTO profile (user_id) VALUES (1);
INSERT INTO user_permission_rel VALUES (1, 1,1,NOW());
INSERT INTO user_permission_rel VALUES (2, 2,1,NOW());
INSERT INTO user_permission_rel VALUES (3, 3,1,NOW());
INSERT INTO user_permission_rel VALUES (4, 4,1,NOW());
INSERT INTO user_permission_rel VALUES (5, 5,1,NOW());
INSERT INTO user_permission_rel VALUES (6, 6,1,NOW());
INSERT INTO user_permission_rel VALUES (7, 7,1,NOW());

INSERT INTO user VALUES (2,'big_willieman','willieman', password('go'),9000,'no','enabled',NOW());
INSERT INTO profile (user_id) VALUES (2);
INSERT INTO user_permission_rel VALUES (8, 1,2,NOW());
INSERT INTO user_permission_rel VALUES (9, 2,2,NOW());
INSERT INTO user_permission_rel VALUES (10, 3,2,NOW());

INSERT INTO user VALUES (3,'amber.deford@gmail.com','lickalicious', password('zoerose'),9000,'no','enabled',NOW());
INSERT INTO profile (user_id) VALUES (3);
INSERT INTO user_permission_rel VALUES (11, 1,3,NOW());
INSERT INTO user_permission_rel VALUES (12, 2,3,NOW());
INSERT INTO user_permission_rel VALUES (13, 3,3,NOW());
INSERT INTO user_permission_rel VALUES (14, 8,3,NOW());

--1 is the jiyoba watcher at facebook's external id
INSERT INTO external_source VALUES(1,'Jiyoba Watcher at Facebook', "Allows The Jiyoba Watcher application at facebook access to gridnet", NOW());

--1 Associate the root user with Joshua's facebook acocunt
INSERT INTO external_user (external_user_id, external_source_id, user_id, createdDate)  VALUES  ('1312670924', 1, 1, NOW());
INSERT INTO external_user (external_user_id, external_source_id, user_id, createdDate)  VALUES  ('1408660917', 1, 3, NOW());
INSERT INTO external_user (external_user_id, external_source_id, user_id, createdDate)  VALUES  ('1335125100', 1, 2, NOW());

