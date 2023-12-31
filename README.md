# HatFactory

![Paper 1.20.1](https://img.shields.io/badge/Paper-1.20.1-blue)
[![CI](https://github.com/outbreak-team/HatFactory/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/outbreak-team/HatFactory/actions/workflows/gradle.yml)

Плагин, добавляющий в камнерез возможность открытия меню создания шапок, являющихся тыквами с изменённой
custom_model_data.

![Preview](./hats.gif)

## Команды

| Команда                              | Привилегии    | Описание                                                 |
|--------------------------------------|---------------|----------------------------------------------------------|
| `/hats reload`                       | `hats.reload` | Перезагружает конфиги плагина                            |
| `/hats store`                        | `hats.store`  | Открывает меню создания шапок (без камнереза)            |
| `/hats give <hat> [player] [amount]` | `hats.give`   | Выдаёт игроку `player` шапку `hat` в количестве `amount` |

## Конфиг шапок

Чтобы добавить шапку, следует добавить следующую структуру в файл hats.yml

```yaml
# Название, желательно, как у модели
example_hat:
  item:
    material: CARVED_PUMPKIN
    displayname: <rainbow>Крутая шапка
    # Необязательное описание
    lore: |-
      <gray>Строка 1
      <gray>Строка 2
    custom-model-data: 51
  # Необязательный второй ингредиент для крафта шапки
  # ingredient-2: DIAMOND
  # Или, с указанием количества
  ingredient-2:
    material: DIAMOND
    amount: 3
  # Если не указать никаких разрешений, шапку сможет создать кто угодно.
  # Разрешение `hats.имя_шапки` позволяет создавать шапку `имя_шапки`
  # вне зависимости от указанных здесь разрешений.
  permissions:
    - group.owner

```
