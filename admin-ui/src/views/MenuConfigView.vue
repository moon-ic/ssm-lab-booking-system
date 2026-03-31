<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMenuConfigs, listIcons, updateMenuConfig } from '@/api/menu-config'
import { listPermissions } from '@/api/roles'
import type { MenuConfigItem, UpdateMenuConfigPayload } from '@/types/menu-config'
import type { PermissionItem } from '@/types/role'

const loading = ref(false)
const detailLoading = ref(false)
const saveDialogVisible = ref(false)
const saveFormRef = ref()

const menus = ref<MenuConfigItem[]>([])
const icons = ref<string[]>([])
const permissions = ref<PermissionItem[]>([])

const selectedMenuId = ref<number>()
const selectedMenu = ref<MenuConfigItem | null>(null)

const saveForm = reactive<UpdateMenuConfigPayload & { menuId?: number }>({
  menuId: undefined,
  menuName: '',
  path: '',
  icon: '',
  permissionCode: ''
})

const saveRules = {
  menuName: [{ required: true, message: 'Please enter menu name', trigger: 'blur' }],
  path: [{ required: true, message: 'Please enter route path', trigger: 'blur' }],
  icon: [{ required: true, message: 'Please choose an icon', trigger: 'change' }],
  permissionCode: [{ required: true, message: 'Please choose a permission code', trigger: 'change' }]
}

const permissionOptions = computed(() => permissions.value.filter((item) => item.type === 'ACTION'))

const summaryCards = computed(() => [
  {
    title: 'Menus',
    value: menus.value.length,
    description: 'Configurable menu entries currently registered.'
  },
  {
    title: 'Icons',
    value: icons.value.length,
    description: 'Built-in icon names available for menu configuration.'
  },
  {
    title: 'Permission codes',
    value: permissionOptions.value.length,
    description: 'Action codes that can be linked to a menu item.'
  }
])

async function loadInitialData() {
  loading.value = true

  try {
    const [menuList, iconList, permissionList] = await Promise.all([
      listMenuConfigs(),
      listIcons(),
      listPermissions('ACTION')
    ])

    menus.value = menuList
    icons.value = iconList
    permissions.value = permissionList

    if (menuList.length > 0) {
      await selectMenu(menuList[0].menuId)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load menu configuration')
  } finally {
    loading.value = false
  }
}

async function selectMenu(menuId: number) {
  selectedMenuId.value = menuId
  detailLoading.value = true

  try {
    selectedMenu.value = menus.value.find((item) => item.menuId === menuId) ?? null
  } finally {
    detailLoading.value = false
  }
}

function openEditDialog(menu: MenuConfigItem) {
  saveForm.menuId = menu.menuId
  saveForm.menuName = menu.menuName
  saveForm.path = menu.path
  saveForm.icon = menu.icon
  saveForm.permissionCode = menu.permissionCode
  saveDialogVisible.value = true
}

async function submitSave() {
  await saveFormRef.value?.validate()
  if (!saveForm.menuId) {
    return
  }

  try {
    const payload: UpdateMenuConfigPayload = {
      menuName: saveForm.menuName,
      path: saveForm.path,
      icon: saveForm.icon,
      permissionCode: saveForm.permissionCode
    }

    const updated = await updateMenuConfig(saveForm.menuId, payload)
    menus.value = menus.value.map((item) => (item.menuId === updated.menuId ? updated : item))
    selectedMenu.value = updated
    saveDialogVisible.value = false
    ElMessage.success('Menu configuration updated')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Save failed')
  }
}

onMounted(() => {
  void loadInitialData()
})
</script>

<template>
  <div class="menu-config-page">
    <section class="hero-card">
      <span class="eyebrow">Menu Module</span>
      <h2>Menu and icon configuration</h2>
      <p>
        This page mirrors the backend menu-config contract for super admins: inspect registered menus, choose built-in
        icons, and keep menu titles, paths, and permission codes aligned.
      </p>
    </section>

    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.title" class="summary-card">
        <span>{{ card.title }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.description }}</p>
      </article>
    </section>

    <section class="workspace-grid" v-loading="loading">
      <article class="menu-list-card">
        <div class="card-header">
          <div>
            <h3>Menu Registry</h3>
            <p>{{ menus.length }} configured entries</p>
          </div>
        </div>

        <div class="menu-list">
          <button
            v-for="menu in menus"
            :key="menu.menuId"
            type="button"
            class="menu-item"
            :class="{ 'is-active': selectedMenuId === menu.menuId }"
            @click="selectMenu(menu.menuId)"
          >
            <div class="menu-item-top">
              <strong>{{ menu.menuName }}</strong>
              <ElTag size="small">{{ menu.icon }}</ElTag>
            </div>
            <span>{{ menu.path }}</span>
            <small>{{ menu.permissionCode }}</small>
          </button>
        </div>
      </article>

      <article class="menu-detail-card">
        <div class="card-header">
          <div>
            <h3>Menu Detail</h3>
            <p v-if="selectedMenu">ID {{ selectedMenu.menuId }}</p>
          </div>
          <ElButton type="primary" :disabled="!selectedMenu" @click="selectedMenu && openEditDialog(selectedMenu)">
            Edit Menu
          </ElButton>
        </div>

        <ElSkeleton v-if="detailLoading" :rows="6" animated />
        <template v-else-if="selectedMenu">
          <div class="detail-grid">
            <article>
              <strong>Menu name</strong>
              <span>{{ selectedMenu.menuName }}</span>
            </article>
            <article>
              <strong>Route path</strong>
              <span>{{ selectedMenu.path }}</span>
            </article>
            <article>
              <strong>Icon</strong>
              <span>{{ selectedMenu.icon }}</span>
            </article>
            <article>
              <strong>Permission code</strong>
              <span>{{ selectedMenu.permissionCode }}</span>
            </article>
          </div>

          <div class="preview-block">
            <h4>Preview</h4>
            <div class="preview-card">
              <ElTag>{{ selectedMenu.icon }}</ElTag>
              <div>
                <strong>{{ selectedMenu.menuName }}</strong>
                <span>{{ selectedMenu.path }}</span>
              </div>
            </div>
          </div>

          <div class="options-grid">
            <section class="option-panel">
              <h4>Available icons</h4>
              <div class="chip-list">
                <ElTag v-for="icon in icons" :key="icon" :type="icon === selectedMenu.icon ? 'primary' : 'info'">
                  {{ icon }}
                </ElTag>
              </div>
            </section>

            <section class="option-panel">
              <h4>Available permission codes</h4>
              <div class="chip-list">
                <ElTag
                  v-for="permission in permissionOptions"
                  :key="permission.permissionId"
                  :type="permission.permissionCode === selectedMenu.permissionCode ? 'success' : 'info'"
                >
                  {{ permission.permissionCode }}
                </ElTag>
              </div>
            </section>
          </div>
        </template>
      </article>
    </section>

    <ElDialog v-model="saveDialogVisible" title="Edit Menu Configuration" width="520px">
      <ElForm ref="saveFormRef" :model="saveForm" :rules="saveRules" label-position="top">
        <ElFormItem label="Menu Name" prop="menuName">
          <ElInput v-model="saveForm.menuName" placeholder="Enter menu name" />
        </ElFormItem>
        <ElFormItem label="Route Path" prop="path">
          <ElInput v-model="saveForm.path" placeholder="Enter route path" />
        </ElFormItem>
        <ElFormItem label="Icon" prop="icon">
          <ElSelect v-model="saveForm.icon" filterable placeholder="Choose built-in icon">
            <ElOption v-for="icon in icons" :key="icon" :label="icon" :value="icon" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="Permission Code" prop="permissionCode">
          <ElSelect v-model="saveForm.permissionCode" filterable placeholder="Choose permission code">
            <ElOption
              v-for="permission in permissionOptions"
              :key="permission.permissionId"
              :label="`${permission.permissionName} / ${permission.permissionCode}`"
              :value="permission.permissionCode"
            />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="saveDialogVisible = false">Cancel</ElButton>
        <ElButton type="primary" @click="submitSave">Save</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.menu-config-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.summary-card,
.menu-list-card,
.menu-detail-card {
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.hero-card h2 {
  margin: 10px 0 12px;
  font-size: 30px;
}

.hero-card p,
.summary-card p {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: #155e75;
  background: #cffafe;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: grid;
  gap: 10px;
}

.summary-card span {
  color: #64748b;
}

.summary-card strong {
  font-size: 28px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
}

.card-header,
.menu-item-top,
.preview-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.card-header {
  margin-bottom: 18px;
}

.card-header h3 {
  margin: 0;
}

.card-header p,
.menu-item span,
.menu-item small,
.detail-grid span,
.preview-card span {
  color: #64748b;
}

.menu-list {
  display: grid;
  gap: 12px;
}

.menu-item {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid transparent;
  border-radius: 18px;
  text-align: left;
  cursor: pointer;
  background: #f8fafc;
}

.menu-item.is-active {
  border-color: rgba(14, 116, 144, 0.28);
  background: #ecfeff;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-grid article,
.option-panel,
.preview-card {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.detail-grid article {
  display: grid;
  gap: 6px;
}

.preview-block,
.options-grid {
  margin-top: 24px;
}

.preview-block h4,
.option-panel h4 {
  margin: 0 0 12px;
}

.preview-card {
  justify-content: flex-start;
}

.preview-card div {
  display: grid;
  gap: 6px;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 1100px) {
  .summary-grid,
  .workspace-grid,
  .detail-grid,
  .options-grid {
    grid-template-columns: 1fr;
  }
}
</style>
