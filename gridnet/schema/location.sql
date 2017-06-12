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
  watch_type enum('track','position','ball') NOT NULL default 'track',
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE geo_quadrangle (
  geo_quadrangle_id BIGINT auto_increment PRIMARY KEY,
  geo_quadrangle_uid varchar(256) UNIQUE,
  top_latitude DOUBLE NOT NULL,
  bottom_latitude DOUBLE NOT NULL,
  west_longitude DOUBLE NOTe NULL,
  east_longitude DOUBLE NOT NULL,
  CONSTRAINT unique_bounds UNIQUE(top_latitude, bottom_latitude, west_longitude, east_longitude)
) ENGINE=InnoDB ROW_FORMAT=FIXED;

CREATE TABLE kickstarter_backer_token (
  backer_token_id BIGINT auto_increment PRIMARY KEY,
  geo_quadrangle_allotment INTEGER NOT NULL,
  geo_quadrangle_allotment_used INTEGER NOT NULL,
  backer_token varchar(128) NOT NULL
) ENGINE=InnoDB ROW_FORMAT=FIXED;

--defunct is set true if the satus is denied, expired or reversed
CREATE TABLE stewardship_purchase_transaction (
  stewardship_purchase_transaction_id BIGINT auto_increment PRIMARY KEY,
  user_id BIGINT NOT NULL,
  amount DOUBLE NOT NULL,
  defunct BOOLEAN NOT NULL default false,
  status enum('standby','pending','complete', 'denied', 'reversed', 'expired') NOT NULL default 'standby',
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB ROW_FORMAT=FIXED;

CREATE TABLE stewardship_purchase_transaction_geo_quadrangle_rel (
  stewardship_purchase_transaction_id BIGINT NOT NULL,
  geo_quadrangle_id BIGINT NOT NULL,
  FOREIGN KEY (geo_quadrangle_id) REFERENCES geo_quadrangle (geo_quadrangle_id),
  FOREIGN KEY (stewardship_purchase_transaction_id) REFERENCES stewardship_purchase_transaction (stewardship_purchase_transaction_id)
) ENGINE=InnoDB ROW_FORMAT=FIXED;

CREATE TABLE kickstarter_backer_token_geo_quadrangle_rel (
  kickstarter_backer_token_id BIGINT NOT NULL,
  geo_quadrangle_id BIGINT NOT NULL,
  FOREIGN KEY (geo_quadrangle_id) REFERENCES geo_quadrangle (geo_quadrangle_id),
  FOREIGN KEY (kickstarter_backer_token_id) REFERENCES kickstarter_backer_token (backer_token_id)
) ENGINE=InnoDB ROW_FORMAT=FIXED;

CREATE TABLE home_base (
  home_base_id BIGINT auto_increment PRIMARY KEY,
  user_id BIGINT NOT NULL,
  geo_quadrangle_id BIGINT NOT NULL,
  home_base_sub_unit integer NOT NULL,
  FOREIGN KEY (geo_quadrangle_id) REFERENCES geo_quadrangle (geo_quadrangle_id),
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB ROW_FORMAT=FIXED;


CREATE TABLE transaction (
  transaction_id BIGINT auto_increment PRIMARY KEY,
  user_id BIGINT NOT NULL,
  paypal_transaction_id BIGINT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB ROW_FORMAT=FIXED;

CREATE TABLE stewardship (
  stewardship_id BIGINT auto_increment PRIMARY KEY,
  user_id BIGINT NOT NULL,
  transaction_id BIGINT NOT NULL,
  geo_quadrangle_id BIGINT NOT NULL,
  FOREIGN KEY (transaction_id) REFERENCES transaction (transaction_id),
  FOREIGN KEY (user_id) REFERENCES user (user_id),
  FOREIGN KEY (geo_quadrangle_id) REFERENCES geo_quadrangle (geo_quadrangle_id)
) ENGINE=InnoDB ROW_FORMAT=FIXED;

/*
--CREATE TABLE stewardship_transaction_rel (
   --stewardship_transaction_id BIGINT auto_increment PRIMARY KEY,
   --transaction_id BIGINT auto_increment PRIMARY KEY,
   --stewardship_id BIGINT auto_increment PRIMARY KEY,
   --FOREIGN KEY (transaction_id) REFERENCES transaction (transaction_id),
   --FOREIGN KEY (stewardship_id) REFERENCES stewardship (stewardship_id)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
*/

CREATE TABLE track_log (
  track_log_id BIGINT auto_increment PRIMARY KEY,
  track_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
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
  small_icon_url varchar(256),
  icon_url varchar(256),
  image_url varchar(256),
  element enum('earth','water','air', 'fire') DEFAULT 'earth' NOT NULL,
  mass DOUBLE NOT NULL DEFAULT 0.0,
  diameter DOUBLE NOT NULL DEFAULT 0.0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE ball (
  ball_id BIGINT auto_increment PRIMARY KEY,
  ball_spec_id BIGINT,
  name varchar(64),
  description varchar(256),
  possessor_id BIGINT,
  creator_id BIGINT,
  created_date timestamp NOT NULL,
  FOREIGN KEY (ball_spec_id) REFERENCES ball_spec(ball_spec_id),
  FOREIGN KEY (possessor_id) REFERENCES user (user_id),
  FOREIGN KEY (creator_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

--Many to Many user ball table for creators of balls.  Not possessors.
-- CREATE TABLE ball_user_rel (
  -- ball_user_rel_id BIGINT auto_increment PRIMARY KEY,
  -- ball_id BIGINT,
  -- user_id BIGINT,
  -- FOREIGN KEY (ball_id) REFERENCES ball(ball_id),
  -- FOREIGN KEY (user_id) REFERENCES user(user_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE ball_track_log (
  ball_track_log_id BIGINT auto_increment PRIMARY KEY,
  ball_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  interest BIGINT NOT NULL DEFAULT 1,
  event_type enum('launch','landing','hand-off', 'drop', 'creation','pick-up') NOT NULL,
  time DATETIME,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  altitude FLOAT NOT NULL DEFAULT 0.0,
  h_accuracy FLOAT NOT NULL DEFAULT -1,
  v_accuracy FLOAT NOT NULL DEFAULT -1,
  location_method INTEGER NOT NULL DEFAULT -1,
  FOREIGN KEY (ball_id) REFERENCES ball(ball_id),
  FOREIGN KEY (user_id) REFERENCES user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

INSERT INTO permission (domain, label, description, lastModified) 
       VALUES ('watchme','watch_users','Allows a user to watch another user',NOW());
INSERT INTO permission (domain, label, description, lastModified) 
       VALUES ('watchme','grab_user_balls','Allows a user to grab another\'s balls',NOW());


--Allows amber to watch Joshua
INSERT INTO user_rel VALUES (1,3,2,8);
--Allows amber to grab Joshua's balls.
INSERT INTO user_rel VALUES (2,3,2,9);



INSERT INTO ball_spec VALUES(1,
                            'Original Ball', 
                            'dotGray', 
                            'grayBall', 
                            'grayBall', 
                            'earth', 
                            1, 
                            10);

INSERT INTO ball VALUES(1,1,'Free Steel from South Austin TX',null,null,1,'2007-01-01 00:00:01');
INSERT INTO ball_track_log VALUES(1,1,1,1,'creation','2007-01-01 00:00:01',30.2158, -97.856, 0, 1, 1, 1);

INSERT INTO ball VALUES(2,1,'Free Steel from Missouri',null,null,1,'2007-01-01 00:00:01');
INSERT INTO ball_track_log VALUES(2,2,1,1,'creation','2007-01-01 00:00:01',30.392266756,-97.101425943,0,1,1,1);

INSERT INTO ball VALUES(3,1,'Free Steel from Indonesia',null,null,3,'2007-01-01 00:00:01');
INSERT INTO ball_track_log VALUES(3,3,1,1,'creation','2007-01-01 00:00:01',1.3, 103.83333,0,1,1,1);

INSERT INTO ball VALUES(4,1,'Big Willi\'s',null,null,2,'2007-01-01 00:00:01');
INSERT INTO ball_track_log VALUES(4,4,1,1,'creation','2007-01-01 00:00:01',32.627261, -97.109472, 0, 1, 1, 1);
