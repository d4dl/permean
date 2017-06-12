-- Units are all SI units. (meter, kg, seconds etc.) 
--
--Conventions:  id's are <tablename>_id.  
--              Foreign key columns are the same name as the column they key on.
--              This makes sql joins much easier to build.
--              Relationship mapping tables are <table1>_<table2>_rel


CREATE TABLE track (
  track_id BIGINT auto_increment PRIMARY KEY,
  name varchar(64),
  user_id BIGINT NOT NULL,
  watch_type enum('track','position') NOT NULL default 'track',
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

--A square area representing an area owned by a user
CREATE TABLE user_square (
  user_home_id BIGINT auto_increment PRIMARY KEY,
  user_id BIGINT NOT NULL,
  upper_left_latitude DOUBLE NOT NULL,
  upper_left_longitude DOUBLE NOT NULL,
  lower_right_latitude DOUBLE NOT NULL,
  lower_right_longitude DOUBLE NOT NULL,
  is_home BOOLEAN,
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE track_log (
  track_log_id BIGINT auto_increment PRIMARY KEY,
  track_id BIGINT NOT NULL,
  milestone INT NULL,
  time DATETIME,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  altitude FLOAT NOT NULL DEFAULT 0.0,
  h_accuracy FLOAT NOT NULL DEFAULT -1,
  v_accuracy FLOAT NOT NULL DEFAULT -1,
  speed FLOAT NOT NULL DEFAULT 0.0,
  location_method INTEGER NOT NULL DEFAULT -1,
  FOREIGN KEY (track_id) REFERENCES track(track_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE milestone (
  milestone_id BIGINT auto_increment PRIMARY KEY,
  track_log_id BIGINT NOT NULL,
  FOREIGN KEY (track_log_id) REFERENCES track_log(track_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

--Balls have attributes, like a material.  Think of a big steel ball or
--a ginormous wooden ball.
CREATE TABLE ball_spec (
  ball_spec_id BIGINT auto_increment PRIMARY KEY,
  name varchar(64),
  mass DOUBLE NOT NULL DEFAULT 0.0,
  diameter DOUBLE NOT NULL DEFAULT 0.0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE ball (
  ball_id BIGINT auto_increment PRIMARY KEY,
  ball_spec_id BIGINT,
  name varchar(64),
  last_modified timestamp default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, 
  created_date timestamp NOT NULL,
  FOREIGN KEY (ball_spec_id) REFERENCES ball_spec(ball_spec_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE ball_track_log (
  ball_track_log_id BIGINT auto_increment PRIMARY KEY,
  ball_id BIGINT NOT NULL,
  time DATETIME,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  altitude FLOAT NOT NULL DEFAULT 0.0,
  h_accuracy FLOAT NOT NULL DEFAULT -1,
  v_accuracy FLOAT NOT NULL DEFAULT -1,
  location_method INTEGER NOT NULL DEFAULT -1,
  FOREIGN KEY (ball_id) REFERENCES ball(ball_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

--Many to Many user ball table
CREATE TABLE ball_user_rel (
  ball_user_rel_id BIGINT auto_increment PRIMARY KEY,
  ball_id BIGINT,
  user_id BIGINT,
  FOREIGN KEY (ball_id) REFERENCES ball(ball_id),
  FOREIGN KEY (user_id) REFERENCES user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


INSERT INTO permission VALUES (7,'watchme','watch_users','Allows a user to watch another user',NOW());
INSERT INTO permission VALUES (8,'watchme','grab_user_balls','Allows a user to grab another\'s balls',NOW());


--Allows amber to watch Joshua
INSERT INTO user_rel VALUES (1,3,2,7);
--Allows amber to grab Joshua's balls.
INSERT INTO user_rel VALUES (2,3,2,8);



INSERT INTO ball_spec VALUES(1,'1Kg 10m Metal', 1, 10);

INSERT INTO ball VALUES(1,1,'Free Steel from South Austin TX','2007-01-01 00:00:01','2007-01-01 00:00:01');
INSERT INTO ball_track_log VALUES(1,1,'2007-01-01 00:00:01',30.2158, -97.856, 0, 1, 1, 1);

INSERT INTO ball VALUES(2,1,'Free Steel from Indonesia','2007-01-01 00:00:01','2007-01-01 00:00:01');
INSERT INTO ball_track_log VALUES(2,2,'2007-01-01 00:00:01',1.3, 103.83333,0,1,1,1);
