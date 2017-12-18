package at.sunplugged.multicontacttable.database.di;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;

import at.sunplugged.multicontacttable.database.services.DatabaseService;
import at.sunplugged.multicontacttable.database.services.SettingsService;
import at.sunplugged.multicontacttable.database.services.impl.DatabaseServiceImpl;
import at.sunplugged.multicontacttable.database.services.impl.SettingsServiceImpl;

public class InjectorContext extends AbstractModule{

	@Override
	protected void configure() {
		bind(DatabaseService.class).to(DatabaseServiceImpl.class);
		bind(SettingsService.class).to(SettingsServiceImpl.class);
	}

}
