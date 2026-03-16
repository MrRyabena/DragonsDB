package collection;

import dragon.Dragon;
import java.util.Set;

/**
 * help : вывести справку по доступным командам
 * info : вывести в стандартный поток вывода информацию о коллекции (тип, дата
 * инициализации, количество элементов и т.д.)
 * show : вывести в стандартный поток вывода все элементы коллекции в строковом
 * представлении
 * add {element} : добавить новый элемент в коллекцию
 * update id {element} : обновить значение элемента коллекции, id которого равен
 * заданному
 * remove_by_id id : удалить элемент из коллекции по его id
 * clear : очистить коллекцию
 * save : сохранить коллекцию в файл
 * execute_script file_name : считать и исполнить скрипт из указанного файла. В
 * скрипте содержатся команды в таком же виде, в котором их вводит пользователь
 * в интерактивном режиме.
 * exit : завершить программу (без сохранения в файл)
 * remove_greater {element} : удалить из коллекции все элементы, превышающие
 * заданный remove_lower {element} :
 * удалить из коллекции все элементы, меньшие, чем заданный
 * history : вывести последние 8 команд (без их аргументов)
 * count_by_type type : вывести количество элементов, значение поля type которых
 * равно заданному
 * count_greater_than_type type : вывести количество элементов, значение поля
 * type
 * которых больше заданного
 * filter_starts_with_name name : вывести элементы, значение поля name
 * которых начинается с заданной подстроки
 */
public interface Interaction {
    void add(Dragon element);

    void updateId(long id, Dragon element);

    void clear();

    void removeById(long id);

    void removeGreater(Dragon lower);

    int countByType(dragon.DragonType type);

    int countGreaterThanType(dragon.DragonType type);

    Set<Dragon> filterStartsWithName(String sub_name);

    Set<Dragon> getBuffer();
}