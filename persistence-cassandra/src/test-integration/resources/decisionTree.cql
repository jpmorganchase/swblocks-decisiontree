--create keyspace "decisiontree" with replication = {'class': 'SimpleStrategy', 'replication_factor' : 1} and durable_writes = false;

create table active_rules_by_rulesetname (
	rulesetname text,
	id uuid,
	code uuid,
	version int,
	drivers list<text>,
	start timestamp,
	end timestamp,
	outputs list<text>,
	PRIMARY KEY ((rulesetname), id)
	);

create table active_groups_by_rulesetname (
    rulesetname text,
    id uuid,
    name text,
    drivers list<text>,
    start timestamp,
    end timestamp,
    PRIMARY KEY ((rulesetname), id)
    );

create table ruleset_details (
    application text,
    rulesetname text,
    drivers list<text>,
    PRIMARY KEY ((application), rulesetname)
    );

create table active_changesets (
    changesetname text,
    id uuid,
    changeid uuid,
    start timestamp,
    end timestamp,
    rulesetname text,
    initiator text,
    initiatortime timestamp,
    approver text,
    approvertime timestamp,
    activationTime timestamp,
    rulechangetype text,
    ruleid uuid,
    rulecode uuid,
    ruledrivers list<text>,
    ruleoutputs list<text>,
    rulestart timestamp,
    ruleend timestamp,
    vgchangetype text,
    vgid uuid,
    vgname text,
    vgdrivers list<text>,
    vgstart timestamp,
    vgend timestamp,
    vgdrivername text,
    vgrulecodes list<uuid>,
    PRIMARY KEY ((changesetname), changeid, rulechangetype, ruleid, vgchangetype, vgid)
    );

create table draft_changes (
    changeid uuid,
    start timestamp,
    end timestamp,
    rulesetname text,
    initiator text,
    initiatortime timestamp,
    PRIMARY KEY ((rulesetname), changeid)
    );
