'use strict';

const app = document.querySelector('#app');
const api = window.Family;
const tabs = [
  ['overview', 'M3 4v16h18 M7 15l4-5 4 2 5-7 M16 5h4v4', 'Tiến độ'],
  ['skills', 'M12 6c-3-2-6-2-10-1v14c4-1 7-1 10 1 3-2 6-2 10-1V5c-4-1-7-1-10 1z M12 6v14', 'Kỹ năng'],
  ['rewards', 'M3 9h18v4H3z M5 13v8h14v-8 M12 9v12 M12 9C4 9 4 2 8 3c2 0 4 4 4 6zm0 0c8 0 8-7 4-6-2 0-4 4-4 6z', 'Bộ sưu tập'],
  ['settings', 'M3 6h8m5 0h5 M3 17h3m5 0h10 M16 6a2.5 2.5 0 1 1-5 0 2.5 2.5 0 1 1 5 0z M11 17a2.5 2.5 0 1 1-5 0 2.5 2.5 0 1 1 5 0z', 'Cài đặt']
];
const avatarNames = ['Cây thông', 'Người tuyết', 'Bông hoa', 'Ghế dài', 'Thỏ con', 'Đèn lồng'];
let unlocked = false;
let entry = '';
let activeTab = 'overview';
let profileState;
let shownDay = new Date();
let shownMonth = new Date(shownDay.getFullYear(), shownDay.getMonth(), 1);

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}

function button(text, style, action, label) {
  const control = element('button', style, text);
  control.type = 'button';
  if (label) control.setAttribute('aria-label', label);
  control.addEventListener('click', action);
  return control;
}

function image(source, style, alt = '') {
  const artwork = element('img', style);
  artwork.src = source + '.webp';
  artwork.alt = alt;
  return artwork;
}

function backLink(label = api.returnLabel(), action = () => api.close()) {
  const back = button('', 'back-link', action, 'Quay lại ' + label);
  const arrow = element('span', 'arrow', '‹');
  arrow.setAttribute('aria-hidden', 'true');
  back.append(arrow, element('span', null, label));
  return back;
}

function tabIcon(path) {
  const ns = 'http://www.w3.org/2000/svg';
  const icon = document.createElementNS(ns, 'svg');
  icon.setAttribute('viewBox', '0 0 24 24');
  icon.setAttribute('class', 'tab-icon');
  icon.setAttribute('aria-hidden', 'true');
  const drawing = document.createElementNS(ns, 'path');
  drawing.setAttribute('d', path);
  icon.append(drawing);
  return icon;
}

function openGate() {
  app.replaceChildren();
  entry = '';
  const gate = element('section', 'gate');
  const header = element('div', 'gate-head');
  header.append(backLink());
  const card = element('div', 'gate-card');
  const prompt = element('div', 'challenge');
  prompt.append(element('h1', null, 'Góc phụ huynh'),
    element('p', null, 'Đọc ba số bằng chữ, rồi bấm các phím theo thứ tự.'),
    element('strong', 'code-words', api.prompt()));
  const words = prompt.querySelector('.code-words');
  words.setAttribute('aria-live', 'polite');
  const familyArt = image('family', 'challenge-art family');
  familyArt.addEventListener('error', () => {
    familyArt.classList.remove('family');
    if (!familyArt.src.endsWith('/princess.webp')) familyArt.src = 'princess.webp';
  });
  prompt.append(familyArt);
  const slots = element('div', 'code-slots');
  slots.setAttribute('aria-label', 'Các số đã nhập');
  prompt.append(slots);
  for (let i = 0; i < 3; i++) slots.append(element('span', null, '–'));
  const padWrap = element('div', 'pad-wrap');
  const pad = element('div', 'keypad');
  const status = element('div', 'gate-message');
  status.setAttribute('role', 'status');

  const change = key => {
    if (unlocked) return;
    entry = key === 'erase' ? entry.slice(0, -1) : entry + key;
    Array.from(slots.children).forEach((slot, index) => {
      slot.textContent = entry[index] || '–';
      slot.classList.toggle('filled', index < entry.length);
    });
    if (entry.length !== 3) return;
    if (api.verify(entry)) {
      unlocked = true;
      load();
      return;
    }
    entry = '';
    for (const slot of slots.children) {
      slot.textContent = '–';
      slot.classList.remove('filled');
    }
    words.textContent = api.prompt();
    status.textContent = 'Chưa đúng. Mời nhập dãy số mới.';
  };

  for (const digit of '1234567890') pad.append(button(digit, null, () => change(digit)));
  pad.append(button('⌫', 'erase', () => change('erase'), 'Xóa số vừa nhập'));
  padWrap.append(pad, status);
  card.append(prompt, padWrap);
  gate.append(header, card);
  app.append(gate);
  document.addEventListener('keydown', event => {
    if (unlocked || !/^[0-9]$|^Backspace$/.test(event.key)) return;
    event.preventDefault();
    change(event.key === 'Backspace' ? 'erase' : event.key);
  });
}

function load(id = '') {
  profileState = JSON.parse(api.snapshot(id));
  if (!profileState.profiles) return;
  document.documentElement.classList.toggle("reduce-motion", profileState.reduceMotion);
  renderDashboard();
}

function key(date) {
  return String(date.getFullYear()) + String(date.getMonth() + 1).padStart(2, '0')
    + String(date.getDate()).padStart(2, '0');
}

function counts(date) {
  const stamp = key(date);
  return [profileState.days['day_letters_' + stamp] || 0, profileState.days['day_games_' + stamp] || 0];
}

function total(date) {
  const [letters, games] = counts(date);
  return letters + games;
}

function profile() {
  return profileState.profiles.find(child => child.id === profileState.selected);
}

function heading(title) {
  const header = element('header', 'top');
  const left = element('div');
  left.append(element('div', 'brand', 'Vương Quốc Tuyết'), element('h1', null, title));
  header.append(left, backLink());
  return header;
}

function profilesPanel() {
  const section = element('section', 'card family-panel');
  const caption = element('div', 'section-head');
  caption.append(element('h2', null, 'Hồ sơ của bé'), element('small', null,
    profileState.profiles.length + '/' + profileState.max));
  const list = element('div', 'profiles');
  list.setAttribute('aria-label', 'Chọn hồ sơ để xem');
  for (const child of profileState.profiles) {
    const tile = button('', 'profile', () => load(child.id));
    tile.setAttribute('aria-pressed', String(child.id === profileState.selected));
    tile.append(image('prop' + child.avatar), element('span', null, child.name));
    if (child.id === profileState.active) tile.append(element('small', null, 'Đang học'));
    list.append(tile);
  }
  if (profileState.profiles.length < profileState.max) {
    const add = button('', 'profile', () => editProfile());
    add.append(element('span', 'avatar-plus', '+'), element('span', null, 'Thêm hồ sơ'));
    list.append(add);
  }
  const actions = element('div', 'profile-actions');
  actions.append(button('Sửa hồ sơ', 'pill', () => editProfile(profile())));
  if (profileState.selected !== profileState.active)
    actions.append(button('Cho ' + profile().name + ' học', 'primary', () => api.activate(profileState.selected)));
  section.append(caption, list, actions);
  return section;
}

function renderDashboard() {
  const previousScroll = window.scrollY;
  const oldPanel = app.querySelector('.panels.' + activeTab);
  const panelScroll = oldPanel ? oldPanel.scrollTop : 0;
  const wrapper = element('div', 'wrap dashboard');
  wrapper.append(heading('Góc phụ huynh'), profilesPanel());
  const panel = element('section', 'panels ' + activeTab);
  panel.setAttribute('aria-label', tabs.find(tab => tab[0] === activeTab)[2]);
  if (activeTab === 'overview') renderOverview(panel);
  else if (activeTab === 'skills') renderSkills(panel);
  else if (activeTab === 'rewards') renderRewards(panel);
  else renderSettings(panel);
  wrapper.append(panel);
  const nav = element('nav', 'tabs');
  nav.setAttribute('aria-label', 'Mục phụ huynh');
  for (const [id, icon, label] of tabs) {
    const tab = button('', null, () => {
      activeTab = id;
      renderDashboard();
      panelFocus();
    });
    if (activeTab === id) tab.setAttribute('aria-current', 'page');
    tab.append(tabIcon(icon), element('span', null, label));
    nav.append(tab);
  }
  app.replaceChildren(wrapper, nav);
  panel.scrollTop = panelScroll;
  window.scrollTo(0, previousScroll);
}

function panelFocus() {
  const panel = document.querySelector('.panels');
  panel.tabIndex = -1;
  panel.focus();
  panel.scrollIntoView({block: 'nearest'});
}

function renderOverview(panel) {
  const calendar = element('section', 'card calendar');
  const bar = element('div', 'month-bar');
  const monthText = 'Tháng ' + (shownMonth.getMonth() + 1) + ', ' + shownMonth.getFullYear();
  const move = delta => {
    shownMonth = new Date(shownMonth.getFullYear(), shownMonth.getMonth() + delta, 1);
    shownDay = new Date(shownMonth);
    renderDashboard();
    document.querySelector(delta < 0 ? '[aria-label="Tháng trước"]' : '[aria-label="Tháng sau"]').focus();
  };
  bar.append(button('‹', 'icon', () => move(-1), 'Tháng trước'), element('h2', null, monthText),
    button('›', 'icon', () => move(1), 'Tháng sau'));
  const dates = element('div', 'calendar-grid');
  for (const label of ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'])
    dates.append(element('span', 'weekday', label));
  const start = (shownMonth.getDay() + 6) % 7;
  for (let i = 0; i < start; i++) dates.append(element('span'));
  const last = new Date(shownMonth.getFullYear(), shownMonth.getMonth() + 1, 0).getDate();
  for (let i = 1; i <= last; i++) {
    const date = new Date(shownMonth.getFullYear(), shownMonth.getMonth(), i);
    const dayKey = key(date);
    const count = total(date);
    const cell = button(i, count ? 'has-work' : null, () => {
      shownDay = date;
      renderDashboard();
      document.querySelector('[data-date="' + dayKey + '"]').focus();
    }, date.toLocaleDateString('vi-VN') + ' · ' + count + ' hoạt động');
    cell.dataset.date = dayKey;
    cell.setAttribute('aria-pressed', String(key(shownDay) === dayKey));
    if (dayKey === key(new Date())) cell.setAttribute('aria-current', 'date');
    dates.append(cell);
  }
  calendar.append(bar, dates, element('p', 'legend', '● Có bài hoặc trò chơi đã hoàn thành'));
  const side = element('div', 'overview-side');
  const stats = element('section', 'card goal');
  const [letters, games] = counts(shownDay);
  const amount = letters + games;
  const goal = profileState.goal;
  const progress = element('progress');
  progress.max = goal;
  progress.value = Math.min(amount, goal);
  progress.setAttribute('aria-label', amount + '/' + goal + ' hoạt động trong ngày');
  const totals = element('div', 'stat-grid');
  for (const [value, label] of [[letters, 'Bài chữ'], [games, 'Lượt chơi']]) {
    const item = element('div', 'stat');
    item.append(element('strong', null, value), element('small', null, label));
    totals.append(item);
  }
  const selectedToday = key(shownDay) === key(new Date());
  stats.append(element('h2', null, selectedToday ? 'Hôm nay' : 'Ngày ' + shownDay.toLocaleDateString('vi-VN')),
    element('p', null, amount >= goal ? 'Đã đạt mục tiêu trong ngày' : amount + '/' + goal + ' hoạt động · Mục tiêu ngày'),
    progress, totals);
  const weekly = element('section', 'card');
  weekly.append(element('h2', null, '7 ngày gần nhất'));
  const week = element('div', 'week');
  const now = new Date();
  const range = Array.from({length: 7}, (_, i) => new Date(now.getFullYear(), now.getMonth(), now.getDate() - 6 + i));
  const max = Math.max(1, ...range.map(total));
  const formatter = new Intl.DateTimeFormat('vi-VN', {weekday: 'short'});
  for (const date of range) {
    const value = total(date);
    const row = element('div', 'week-row');
    const indicator = element('progress');
    indicator.max = max;
    indicator.value = value;
    indicator.setAttribute('aria-label', date.toLocaleDateString('vi-VN') + ': ' + value);
    row.append(element('span', null, formatter.format(date)), indicator, element('strong', null, value));
    week.append(row);
  }
  weekly.append(week);
  side.append(stats, weekly);
  panel.append(calendar, side);
}

function renderSkills(panel) {
  const letters = element('section', 'card');
  letters.append(element('h2', null, 'Bảng chữ của ' + profile().name),
    element('p', null, profileState.letterTotal + '/' + profileState.letters.length + ' chữ đã hoàn thành'));
  const grid = element('div', 'letter-grid');
  for (const letter of profileState.letters) {
    const tile = element('span', letter.done ? 'letter done' : 'letter', letter.label);
    tile.setAttribute('aria-label', letter.label + (letter.done ? ', đã hoàn thành' : ', chưa hoàn thành'));
    if (letter.done) tile.append(element('span', null, '✓'));
    grid.append(tile);
  }
  letters.append(grid);
  panel.append(letters);
  for (const group of ['Chơi cùng chữ', 'Toán & tư duy']) {
    const games = element('section', 'card list');
    games.append(element('h2', null, group));
    for (const game of profileState.games.filter(item => item.group === group)) {
      const row = element('div', 'list-row');
      row.append(element('span', null, game.label), element('strong', null, game.count + ' lượt'));
      games.append(row);
    }
    panel.append(games);
  }
}

function renderRewards(panel) {
  const card = element('section', 'card');
  const rewards = profileState.rewards;
  card.append(element('h2', null, 'Quà trang trí của ' + profile().name),
    element('p', null, rewards.filter(item => item.done).length + '/' + rewards.length
      + ' món đã mở. Hoàn thành các lượt Chơi cùng chữ để nhận quà.'));
  const grid = element('div', 'reward-grid');
  for (const item of rewards) {
    const token = element('div', item.done ? 'reward' : 'reward locked');
    token.append(image('prop' + item.index), element('h3', null, item.label),
      element('small', null, item.done ? 'Đã mở' : 'Chưa mở'));
    grid.append(token);
  }
  card.append(grid);
  panel.append(card);
}

function renderSettings(panel) {
  const card = element('section', 'card');
  card.append(element('h2', null, 'Mục tiêu hằng ngày'));
  const label = element('label', 'field', 'Số hoạt động mỗi ngày của ' + profile().name);
  const select = element('select');
  for (let amount = 1; amount <= 12; amount++) {
    const option = element('option', null, amount + ' hoạt động');
    option.value = amount;
    select.append(option);
  }
  select.value = profileState.goal;
  const feedback = element('p');
  feedback.setAttribute('role', 'status');
  select.addEventListener('change', () => {
    api.setGoal(profileState.selected, Number(select.value));
    profileState.goal = Number(select.value);
    feedback.textContent = 'Đã lưu mục tiêu.';
  });
  label.append(select);
  card.append(label, feedback);
  const access = element('section', 'card');
  access.append(element('h2', null, 'Hiển thị'));
  const motionLabel = element('label', 'list-row', 'Giảm chuyển động');
  const check = element('input');
  check.type = 'checkbox';
  check.checked = profileState.reduceMotion;
  check.addEventListener('change', () => {
    api.reduceMotion(check.checked);
    profileState.reduceMotion = check.checked;
    document.documentElement.classList.toggle('reduce-motion', check.checked);
  });
  motionLabel.append(check);
  const direction = element('label', 'field', 'Hướng màn hình');
  const orientation = element('select');
  for (const [value, title] of [['system', 'Theo điện thoại'], ['sensor', 'Tự xoay ngang, dọc'], ['portrait', 'Giữ màn hình dọc']]) {
    const option = element('option', null, title);
    option.value = value;
    orientation.append(option);
  }
  orientation.value = profileState.orientation || 'system';
  orientation.onchange = () => {
    profileState.orientation = orientation.value;
    api.setOrientation(orientation.value);
  };
  direction.append(orientation);
  access.append(direction, motionLabel);
  const advance = element('section', 'card');
  advance.append(element('h2', null, 'Tự động học bài tiếp theo'));
  const autoLabel = element('label', 'list-row', 'Tự mở chữ kế tiếp sau khi hoàn thành');
  const autoNext = element('input');
  autoNext.type = 'checkbox';
  autoNext.checked = (profileState.studyDelay || 0) > 0;
  autoLabel.append(autoNext);
  const advanceLabel = element('label', 'field', 'Thời gian chờ trước khi chuyển');
  const delay = element('select');
  for (const seconds of [5, 10, 15, 20, 30]) {
    const option = element('option', null, seconds + ' giây');
    option.value = seconds;
    delay.append(option);
  }
  delay.value = profileState.studyDelay || 10;
  delay.disabled = !autoNext.checked;
  const saved = element('p');
  saved.setAttribute('role', 'status');
  autoNext.addEventListener('change', () => {
    profileState.studyDelay = autoNext.checked ? Number(delay.value || 10) : 0;
    delay.disabled = !autoNext.checked;
    api.setStudyDelay(profileState.selected, profileState.studyDelay);
    saved.textContent = autoNext.checked
      ? 'Đã bật tự chuyển cho ' + profile().name + '.'
      : 'Đã tắt. Con sẽ ở lại màn hình hoàn thành.';
  });
  delay.addEventListener('change', () => {
    profileState.studyDelay = Number(delay.value);
    api.setStudyDelay(profileState.selected, profileState.studyDelay);
    saved.textContent = 'Đã lưu thời gian cho ' + profile().name + '.';
  });
  advanceLabel.append(delay);
  advance.append(autoLabel, advanceLabel,
    element('p', null, 'Bài tiếp theo chỉ mở sau khi bé nghe đủ bài mẫu và hoàn thành hai trò chơi. Khi ôn tập, chữ sẽ đi đúng thứ tự A → Ă → Â…'), saved);
  panel.append(advance, card, access);
}

function editProfile(child) {
  const wrapper = element('div', 'wrap edit');
  const header = element('header', 'top');
  header.append(element('h1', null, child ? 'Sửa hồ sơ' : 'Thêm hồ sơ'),
    backLink('Góc phụ huynh', () => renderDashboard()));
  const card = element('section', 'card');
  const label = element('label', 'field', 'Tên gọi của bé');
  const name = element('input');
  name.maxLength = 24;
  name.autocomplete = 'off';
  name.value = child ? child.name : '';
  label.append(name);
  card.append(label, element('h2', null, 'Chọn hình đại diện'));
  let chosen = child ? child.avatar : 4;
  const avatars = element('div', 'avatars');
  for (let i = 0; i < avatarNames.length; i++) {
    const avatar = button('', null, () => {
      chosen = i;
      Array.from(avatars.children).forEach((item, index) => item.setAttribute('aria-pressed', String(i === index)));
    }, avatarNames[i]);
    avatar.setAttribute('aria-pressed', String(chosen === i));
    avatar.append(image('prop' + i));
    avatars.append(avatar);
  }
  const feedback = element('div', 'gate-message');
  feedback.setAttribute('role', 'status');
  const save = () => {
    const entered = name.value.trim();
    if (!entered) {
      feedback.textContent = 'Mời nhập tên gọi của bé.';
      name.focus();
      return;
    }
    const id = api.saveProfile(child ? child.id : '', entered, chosen);
    if (id) load(id);
    else feedback.textContent = 'Chưa lưu được hồ sơ.';
  };
  name.addEventListener('keydown', event => {
    if (event.key === 'Enter') save();
  });
  card.append(avatars, feedback, button('Lưu hồ sơ', 'primary', save));
  wrapper.append(header, card);
  app.replaceChildren(wrapper);
  window.scrollTo(0, 0);
}

if (api) {
  openGate();
  if (typeof api.ready === 'function') api.ready();
}
