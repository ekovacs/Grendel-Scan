package com.grendelscan.tests.testModuleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.grendelscan.tests.testModules.TestModule;

/**
 * This class represents a collection of TestModules,
 * presumably within the same category. After the class has
 * been finalized, it can return an array of the modules,
 * sorted based on their prerequisites. This is primarily
 * intended for use by a categorizer. Currently, there is no
 * protection against cyclical prerequisites, so be careful.
 * It also assumes that all of the prereqs are present in
 * the collection.
 * 
 * The underlying collection in the class is thread safe,
 * although the finalize method is not (duh).
 * 
 * The initial size of the underlying collection isn't important
 * because it is replaced by a precisely sized collection
 * when the object is finalized.
 * 
 * @author David Byrne
 * 
 */
public class CategoryTestModuleCollection implements Iterable<TestModule>
{
	private List<TestModule>	moduleList;

	public CategoryTestModuleCollection()
	{
		moduleList = Collections.synchronizedList(new ArrayList<TestModule>());
	}

	public boolean add(TestModule e)
	{
		return moduleList.add(e);
	}

	@Override
	public Iterator<TestModule> iterator()
	{
		return moduleList.iterator();
	}

	/**
	 * Call this after all the modules have been added. It
	 * implements (probably poorly) a topological sort to
	 * resolve dependencies. Again, no checks are made for
	 * cyclical prereqs, so be careful. It also assumes
	 * that all of the prereqs are present in the collection.
	 */
	public void resolveDependencies()
	{
		HashMap<Class, TestModule> modulesByClass = new HashMap<Class, TestModule>();
		HashMap<Integer, HashSet<Integer>> remainingPrereqs = new HashMap<Integer, HashSet<Integer>>();
		List<TestModule> sortedModules = Collections.synchronizedList(new ArrayList<TestModule>(moduleList.size()));

		for (TestModule module : moduleList)
		{
			modulesByClass.put(module.getClass(), module);
			HashSet<Class> prereqs = new HashSet<Class>();
			for (Class prereq : module.getPrerequisites())
			{
				prereqs.add(prereq);
			}
		}

		while (remainingPrereqs.size() > 0)
		{
			for (int moduleID : remainingPrereqs.keySet())
			{
				// If there are no more outstanding prereqs
				if (remainingPrereqs.get(moduleID).size() == 0)
				{
					remainingPrereqs.remove(moduleID);
					sortedModules.add(modulesByClass.get(moduleID));

					// Tell everyone that I've been removed
					for (HashSet<Integer> prereqs : remainingPrereqs.values())
					{
						prereqs.remove(moduleID);
					}
				}
			}
		}
		moduleList = sortedModules;
	}

	public int size()
	{
		return moduleList.size();
	}
}
