package org.poolc.hso;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

public class SynchronizeManager {
	
	private static final boolean DEBUG = true;
	
	public static interface Context {
		public String getBaseURL();
		public String getTablePrefix();
		
		public String getCreatePageName();
		public String getUpdatePageName();
		public String getDeletePageName();
		public String getReadPageName();
		public String getListPageName();
	}
	
	public static class DefaultContext implements Context {
		
		private String baseURL;
		private String tablePrefix;
		
		public DefaultContext(String baseURL, String tablePrefix) {
			super();
			this.baseURL = baseURL;
			this.tablePrefix = tablePrefix;
		}

		@Override
		public String getBaseURL() {
			return baseURL;
		}

		@Override
		public String getTablePrefix() {
			return tablePrefix;
		}

		@Override
		public String getCreatePageName() {
			return "/create.php";
		}

		@Override
		public String getUpdatePageName() {
			return "/update.php";
		}

		@Override
		public String getDeletePageName() {
			return "/delete.php";
		}

		@Override
		public String getReadPageName() {
			return "/read.php";
		}

		@Override
		public String getListPageName() {
			return "/list.php";
		}
	}
	
	private static SynchronizeManager instance = null;
	public static synchronized SynchronizeManager getInstance() {
		if (instance == null) {
			throw new RuntimeException("SynchronizeManager doesn't create.");
		}
		return instance;
	}
	
	public static synchronized boolean create(Context context) {
		try {
			instance = new SynchronizeManager(context);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private Context context;
	private SynchronizeManager(Context context) {
		super();
		this.context = context;
	}
	
	
	private String request(String url, String data) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(url).openConnection());
		if (data != null) {
			connection.setDoOutput(true);
			connection.getOutputStream().write(data.getBytes());
		}
		
		if (connection.getResponseCode() == 404) {
			throw new IOException("Cannot find server page.");
		}
		
		StringBuilder content = new StringBuilder();
		Scanner reader = new Scanner(connection.getInputStream());
		while (reader.hasNextLine()) {
			content.append(reader.nextLine()).append('\n');
		}
		if (content.length() > 0)
			content.deleteCharAt(content.length() - 1);
		reader.close();
		connection.disconnect();
		
		if (DEBUG) {
			System.err.println("==== " + url);
			System.err.println("---- " + data);
			System.err.println("%---|" + content.toString().trim() + "|");
			System.err.println("----------------------------------------");
		}
		return content.toString().trim();
	}

	private String getTableName(Object object) {
		return getTableName(object.getClass());
	}
	
	private String getTableName(Class<?> clazz) {
		String tableName = clazz.getSimpleName().toLowerCase();
		if (context.getTablePrefix() != null && !context.getTablePrefix().isEmpty()) {
			tableName = context.getTablePrefix() + "_" + tableName;
		}
		
		return tableName;
	}
	
	private static Field getKeyField(Class<?> clazz) {
		while (!clazz.equals(Object.class)) {
			for (Field field: clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(KeyField.class)) {
					
					if (DEBUG) {
						System.err.println("---- KEY FIELD: " + clazz.getSimpleName() + "(" + field + ")");
					}
					field.setAccessible(true);
					return field;
				}
			}
			clazz = clazz.getSuperclass();
		}
		if (DEBUG) {
			System.err.println("Cannot Find Key Field in " + clazz.getCanonicalName());
		}
		throw new NoKeyFieldException();
	}

	private static Iterable<Field> getSerializableFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<Field>();
		while (!clazz.equals(Object.class)) {
			for (Field field: clazz.getDeclaredFields()) {
				int modifier = field.getModifiers();
				if (Modifier.isFinal(modifier) || Modifier.isStatic(modifier) || Modifier.isTransient(modifier))
					continue;
				
				if (!field.isAnnotationPresent(ClientField.class)) {
					fields.add(field);
				}
			}
			clazz = clazz.getSuperclass();
		}
		return fields;
	}
	
	private static Map<String, Field> getFieldMap(Class<?> clazz) {
		Map<String, Field> map = new HashMap<String, Field>();
		while (!clazz.equals(Object.class)) {
			for (Field field: clazz.getDeclaredFields()) {
				int modifier = field.getModifiers();
				if (Modifier.isFinal(modifier) || Modifier.isStatic(modifier) || Modifier.isTransient(modifier))
					continue;
				
				if (field.isAnnotationPresent(ClientField.class))
					continue;
				
				if (!map.containsKey(field.getName()))
					map.put(field.getName(), field);
			}
			clazz = clazz.getSuperclass();
		}
		return map;
	}

	private String serialize(Object object, String... allowFields) {
		Field keyField = getKeyField(object.getClass());
		Object keyValue = null;
		try {
			keyValue = keyField.get(object);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		boolean notInitializedKey = (keyValue == null 
				|| (keyValue.getClass().equals(int.class) && int.class.cast(keyValue).equals(0))
				|| (keyValue.getClass().equals(long.class) && long.class.cast(keyValue).equals(0L)));
		
		Set<String> allowSet = null;
		if (allowFields.length > 0) {
			allowSet = new HashSet<String>();
			for (String field: allowFields) {
				allowSet.add(field);
			}
			allowSet.add(keyField.getName());
		}
		
		StringBuilder message = new StringBuilder();
		message.append("TABLE_NAME=").append(getTableName(object));
		for (Field field: getSerializableFields(object.getClass())) {
			if (allowSet != null && !allowSet.contains(field.getName())) continue;
			
			// if current field equals key field, skip that field because create statement doesn't want key value. 
			
			if (notInitializedKey && field.equals(keyField)) continue;
			try {
				field.setAccessible(true);
				message.append('&').append(field.getName()).append('=');
				if (field.getType().equals(boolean.class)) {
					message.append(boolean.class.cast(field.get(object))? '1': '0');
					
				} else {
					message.append(field.get(object));
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (DEBUG) {
			System.err.println(" * SERIALIZE MESSAGE: " + object.toString());
			System.err.println(message.toString());
			System.err.println("------------------------------------------");
		}
		return message.toString();
	}
	
	private boolean deserialize(Object object, String message, String... allowFields) {
		if (message.isEmpty())
			return false;
		
		Set<String> allowSet = null;
		if (allowFields.length > 0) {
			allowSet = new HashSet<String>();
			for (String field: allowFields) {
				allowSet.add(field);
			}
		}
		
		if (DEBUG) {
			System.err.println(" * DESERIALIZE MESSAGE: " + message.toString());
			System.err.println("------------------------------------------");
		}
		
		boolean success = true;
		Map<String, Field> fieldMap = getFieldMap(object.getClass());
		StringTokenizer tokenizer = new StringTokenizer(message, "&");
		while (tokenizer.hasMoreTokens()) {
			String set = tokenizer.nextToken();
			String name = set.substring(0, set.indexOf('='));
			String value = set.substring(set.indexOf('=') + 1);
			
			if (allowSet != null && !allowSet.contains(name)) continue;

			try {
				Field field = fieldMap.get(name);
				if (field == null) {
					throw new NoSuchFieldException(name);
				}
				field.setAccessible(true);
				Class<?> type = field.getType();
				
				if (DEBUG) {
					System.err.println(" * DESERIALIZING: " + name + "=" + value + "(" + type.getSimpleName() + ")");
				}
				
				if (type.equals(boolean.class)) {
					field.set(object, value.equals("1"));
					
				} else if (type.equals(char.class)) {
					field.set(object, value.charAt(0));
					
				} else if (type.equals(byte.class)) {
					field.set(object, Byte.parseByte(value));
					
				} else if (type.equals(short.class)) {
					field.set(object, Short.parseShort(value));
					
				} else if (type.equals(int.class)) {
					field.set(object, Integer.parseInt(value));
					
				} else if (type.equals(float.class)) {
					field.set(object, Float.parseFloat(value));
					
				} else if (type.equals(double.class)) {
					field.set(object, Double.parseDouble(value));
					
				} else if (type.equals(String.class)) {
					if (value.equalsIgnoreCase("null")) value = null;
					field.set(object, value);
					
				} else {
					throw new Exception("Cannot find match with " + type.toString() + " and " + value);
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}  
		}
		
		if (DEBUG) {
			System.err.println(" * DESERIALIZE COMPLETE: " + object.toString());
			System.err.println("------------------------------------------");
		}
		return success;
	}
	
	public boolean create(Object object) {
		try {
			if (DEBUG) {
				System.err.println(" * CREATE OBJECT: " + object.getClass() + ": " + object.toString());
				System.err.println("------------------------------------------");
			}
			return deserialize(object, request(context.getBaseURL() + context.getCreatePageName(), serialize(object)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean synchronize(Object object, String[] allowSerializeFields, String[] allowDeserializeFields) {
		Field keyField = getKeyField(object.getClass());
		
		try {
			if (DEBUG) {
				System.err.println(" * SYNC     OBJECT: " + object.getClass() + ": " + object.toString());
				System.err.println(" * ALLOW SE FIELDS: " + Arrays.toString(allowSerializeFields));
				System.err.println(" * ALLOW DE FIELDS: " + Arrays.toString(allowSerializeFields));
				System.err.println("------------------------------------------");
			}
			return deserialize(object, request(context.getBaseURL() + context.getUpdatePageName(), "KEY_NAME=" + keyField.getName() + "&" + serialize(object, allowSerializeFields)), allowDeserializeFields);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean update(Object object, String... allowFields) {
		Field keyField = getKeyField(object.getClass());
		
		try {
			if (DEBUG) {
				System.err.println(" * UPDATE OBJECT: " + object.getClass() + ": " + object.toString());
				System.err.println(" * ALLOW  FIELDS: " + Arrays.toString(allowFields));
				System.err.println("------------------------------------------");
			}
			return deserialize(object, request(context.getBaseURL() + context.getUpdatePageName(), "KEY_NAME=" + keyField.getName() + "&" + serialize(object, allowFields)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean read(Object object, Object key, String... allowFields) {
		Field keyField = getKeyField(object.getClass());
		
		if (DEBUG) {
			System.err.println(" * READ OBJECT: " + keyField.getName() + "(" + key + ")");
			System.err.println(" * ALLOW  FIELDS: " + Arrays.toString(allowFields));
			System.err.println("------------------------------------------");
		}
		try {
			StringBuilder message = new StringBuilder();
			message.append("TABLE_NAME=").append(getTableName(object)).append('&');
			message.append("KEY_NAME=").append(keyField.getName()).append('&');
			message.append(keyField.getName()).append('=').append(key);
			return deserialize(object, request(context.getBaseURL() + context.getReadPageName(), message.toString()), allowFields);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean find(Object object, String key1, String value1, String... others) {
		StringBuilder query = new StringBuilder();
		query.append("TABLE_NAME=").append(getTableName(object)).append('&');
		query.append(key1).append('=').append(value1);
		for (int i = 0; i < others.length; i += 2) {
			query.append('&').append(others[i]).append('=').append(others[i + 1]);
		}
		if (DEBUG) {
			System.err.println(" * FIND OBJECT: " + object.getClass());
			System.err.println(" * CONDITIONS : " + query.toString());
			System.err.println("------------------------------------------");
		}
		try {
			return deserialize(object, request(context.getBaseURL() + context.getReadPageName(), query.toString()));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public <T> List<T> list(Class<T> clazz, String... conditions) {
		StringBuilder query = new StringBuilder();
		query.append("TABLE_NAME=").append(getTableName(clazz)).append('&');
		if (conditions.length > 0) {
			query.append(conditions[0]).append('=').append(conditions[1]);
		}
		for (int i = 2; i < conditions.length; i += 2) {
			query.append('&').append(conditions[i]).append('=').append(conditions[i + 1]);
		}
		if (DEBUG) {
			System.err.println(" * LIST OBJECT: " + clazz);
			System.err.println(" * CONDITIONS : " + query.toString());
			System.err.println("------------------------------------------");
		}
		List<T> list = new ArrayList<T>();
		try {
			String message = request(context.getBaseURL() + context.getListPageName(), query.toString());
			StringTokenizer tokenizer = new StringTokenizer(message, "\n");
			while (tokenizer.hasMoreTokens()) {
				T object = clazz.newInstance();
				if (deserialize(object, tokenizer.nextToken())) {
					list.add(object);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public boolean delete(Object object) {
		try {
			return delete(object.getClass(), getKeyField(object.getClass()).get(object));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean delete(Class<?> clazz, Object key) {
		Field keyField = getKeyField(clazz);
		
		if (DEBUG) {
			try {
				System.err.println(" * DELETE OBJECT: " + keyField.getName() + "(" + key + ")");
				System.err.println("------------------------------------------");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			StringBuilder message = new StringBuilder();
			message.append("TABLE_NAME=").append(getTableName(clazz)).append('&');
			message.append("KEY_NAME=").append(keyField.getName()).append('&');
			message.append(keyField.getName()).append('=').append(key);
			request(context.getBaseURL() + context.getDeletePageName(), message.toString());
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
