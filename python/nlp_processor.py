"""
Обработка естественного языка для команд в чате
Преобразует текст команд в действия AI
"""

import re
from enum import Enum
from typing import Dict, Tuple, Optional

class ActionType(Enum):
    """Типы доступных действий"""
    BUILD = "build_structure"
    SPAWN = "spawn_mob"
    GENERATE = "generate_ore"
    TELEPORT = "teleport"
    HELP = "help"
    STATUS = "status"
    UNKNOWN = "unknown"

class NLPProcessor:
    """Обработчик команд на естественном языке"""
    
    def __init__(self):
        # Словарь структур
        self.structures = {
            'tower': 'tower',
            'башня': 'tower',
            'house': 'house',
            'дом': 'house',
            'bridge': 'bridge',
            'мост': 'bridge',
            'стена': 'tower',
            'wall': 'tower'
        }
        
        # Словарь мобов
        self.mobs = {
            'zombie': 'zombie',
            'зомби': 'zombie',
            'skeleton': 'skeleton',
            'скелет': 'skeleton',
            'creeper': 'creeper',
            'крипер': 'creeper',
            'spider': 'spider',
            'паук': 'spider',
            'enderman': 'enderman',
            'эндермен': 'enderman'
        }
        
        # Словарь руд
        self.ores = {
            'diamond': 'diamond',
            'алмаз': 'diamond',
            'gold': 'gold',
            'золото': 'gold',
            'iron': 'iron',
            'железо': 'iron',
            'coal': 'coal',
            'уголь': 'coal',
            'emerald': 'emerald',
            'изумруд': 'emerald'
        }
    
    def process_command(self, text: str) -> Dict:
        """
        Обработать команду из чата
        
        Args:
            text: Текст команды из чата
            
        Returns:
            Словарь с типом действия и параметрами
        """
        text = text.lower().strip()
        
        # Удалить точки, запятые и т.д.
        text = re.sub(r'[.,!?;:]', '', text)
        
        # Команды построения
        if self._is_build_command(text):
            return self._parse_build_command(text)
        
        # Команды призыва мобов
        elif self._is_spawn_command(text):
            return self._parse_spawn_command(text)
        
        # Команды генерации руд
        elif self._is_generate_command(text):
            return self._parse_generate_command(text)
        
        # Команды помощи
        elif self._is_help_command(text):
            return {
                'action': ActionType.HELP.value,
                'success': True,
                'message': self._get_help_message()
            }
        
        # Команды статуса
        elif self._is_status_command(text):
            return {
                'action': ActionType.STATUS.value,
                'success': True,
                'message': 'AI сервер активен и готов к работе! ✅'
            }
        
        else:
            return {
                'action': ActionType.UNKNOWN.value,
                'success': False,
                'message': 'Не понимаю команду. Напишите /ai help для справки'
            }
    
    def _is_build_command(self, text: str) -> bool:
        """Проверить, является ли это команда построения"""
        keywords = ['строить', 'build', 'построить', 'сделай', 'создай', 'возведи']
        return any(kw in text for kw in keywords)
    
    def _is_spawn_command(self, text: str) -> bool:
        """Проверить, является ли это команда призыва"""
        keywords = ['призови', 'spawn', 'моб', 'мобов', 'призыв', 'появись']
        return any(kw in text for kw in keywords)
    
    def _is_generate_command(self, text: str) -> bool:
        """Проверить, является ли это команда генерации"""
        keywords = ['генерируй', 'руду', 'generate', 'ore', 'руда', 'копай']
        return any(kw in text for kw in keywords)
    
    def _is_help_command(self, text: str) -> bool:
        """Проверить, является ли это команда помощи"""
        keywords = ['помощь', 'help', 'команды', 'как', 'что']
        return any(kw in text for kw in keywords)
    
    def _is_status_command(self, text: str) -> bool:
        """Проверить, является ли это команда статуса"""
        keywords = ['статус', 'status', 'как дела', 'живой']
        return any(kw in text for kw in keywords)
    
    def _parse_build_command(self, text: str) -> Dict:
        """Разобрать команду построения"""
        structure_type = None
        height = 10  # Высота по умолчанию
        
        # Найти тип структуры
        for keyword, structure in self.structures.items():
            if keyword in text:
                structure_type = structure
                break
        
        # Если тип не найден, использовать tower
        if not structure_type:
            structure_type = 'tower'
        
        # Попытаться найти высоту
        height_match = re.search(r'(\d+)\s*(блок|block|высот|tall)', text)
        if height_match:
            height = int(height_match.group(1))
        
        return {
            'action': ActionType.BUILD.value,
            'structure_type': structure_type,
            'height': min(height, 50),  # Максимум 50 блоков
            'success': True,
            'message': f'🏗️  Строю {structure_type} высотой {height} блоков...'
        }
    
    def _parse_spawn_command(self, text: str) -> Dict:
        """Разобрать команду призыва"""
        mob_type = None
        count = 1  # Количество по умолчанию
        
        # Найти тип моба
        for keyword, mob in self.mobs.items():
            if keyword in text:
                mob_type = mob
                break
        
        if not mob_type:
            mob_type = 'zombie'
        
        # Попытаться найти количество
        count_match = re.search(r'(\d+)\s*(мо|mob|штук)', text)
        if count_match:
            count = int(count_match.group(1))
        
        return {
            'action': ActionType.SPAWN.value,
            'mob_type': mob_type,
            'count': min(count, 20),  # Максимум 20 мобов
            'success': True,
            'message': f'👹 Призываю {count} {mob_type}...'
        }
    
    def _parse_generate_command(self, text: str) -> Dict:
        """Разобрать команду генерации"""
        ore_type = None
        vein_size = 10  # Размер жилы по умолчанию
        
        # Найти тип руды
        for keyword, ore in self.ores.items():
            if keyword in text:
                ore_type = ore
                break
        
        if not ore_type:
            ore_type = 'iron'
        
        # Попытаться найти размер жилы
        size_match = re.search(r'(\d+)\s*(блок|block|размер)', text)
        if size_match:
            vein_size = int(size_match.group(1))
        
        return {
            'action': ActionType.GENERATE.value,
            'ore_type': ore_type,
            'vein_size': min(vein_size, 100),  # Максимум 100 блоков
            'success': True,
            'message': f'⛏️  Генерирую жилу {ore_type}...'
        }
    
    def _get_help_message(self) -> str:
        """Получить справку по командам"""
        return """
§6═══════════════════════════════════════
§e🤖 Команды AI бота§r
§6═══════════════════════════════════════
§bПостроение:§r
  • §eстрой дом§r / §eстрой башню§r
  • §eстрой мост высотой 20 блоков§r

§bПризыв мобов:§r
  • §eпризови зомби§r / §eпризови крипера§r
  • §eпризови 5 скелетов§r

§bГенерация руд:§r
  • §eгенерируй алмазы§r / §eгенерируй золото§r
  • §eгенерируй 50 блоков железа§r

§bОсновные команды:§r
  • §eпомощь§r - показать эту справку
  • §eстатус§r - проверить статус AI
  • §e/ai enable§r - включить AI
  • §e/ai disable§r - выключить AI
§6═══════════════════════════════════════
        """
