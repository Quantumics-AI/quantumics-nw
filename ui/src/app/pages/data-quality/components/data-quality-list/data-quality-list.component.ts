import { Component, OnInit } from '@angular/core';
import { DataQuailtyListResponse } from '../../models/data-quality-list-response';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject } from 'rxjs';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { takeUntil } from 'rxjs/operators';
import { EditRuleComponent } from '../edit-rule/edit-rule.component';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { ViewRunningRulesComponent } from '../view-running-rules/view-running-rules.component';
import { RuleCreationService } from '../../services/rule-creation.service';
import { RuleFilter } from '../../models/rule-filter.model';

@Component({
  selector: 'app-data-quality-list',
  templateUrl: './data-quality-list.component.html',
  styleUrls: ['./data-quality-list.component.scss']
})
export class DataQualityListComponent implements OnInit {

  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;
  public loading:boolean;
  public ruleFilter = [
    { label: 'Inactive', name: 'inactive', selected: false },
    { label: 'Deleted', name: 'delete', selected: false }
  ] as RuleFilter[];

  public dataQualityList: any;
  public ruleCount: any;
  public projectId: number;
  userId: number;
  selectAllChecked: boolean = false;
  anyCheckboxSelected: boolean = false;

  // pagination
  public startIndex: number = 0;
  public pageSize: number = 10;
  public endIndex: number = this.pageSize;
  public page = 1;
  public rulesData: any;

  public ruleStatus: string = 'Active';
  public pageNumebr: number = 1;
  public pageLength: number = 10;

  public ruleId: number;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private modalService: NgbModal,
    private quantumFacade: Quantumfacade,
    private snakbar: SnackbarService,
    private ruleCreationService: RuleCreationService) {
    this.projectId = +this.activatedRoute.snapshot.paramMap.get('projectId');
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
          this.userId = +this.certificateData.user_id;
        }
      });
  }

  ngOnInit(): void {
    this.getRules();
    sessionStorage.clear();
  }

  public getRules(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
      console.log("rules list: ", response);
      this.loading = false;
      this.dataQualityList = response?.result?.content;
      this.ruleCount =  response?.result?.content;
      
    }, (error) => {
      this.loading = false;
    });
  }

  public edit(dataQuality: any): void {

    this.router.navigate([`projects/${this.projectId}/data-quality/edit`],{
      queryParams: {
        dataId: dataQuality.ruleId
      }
    });
  }

  public viewRunning(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality/view-rules`]);
    // const modalRef = this.modalService.open(ViewRunningRulesComponent, { size: 'lg', windowClass: 'modal-size', scrollable: false });
    // modalRef.componentInstance.userId = this.userId;
    // modalRef.componentInstance.projectId = this.projectId;


    // modalRef.result.then((result) => {
      
    // }, (result) => {
      
    // });
  }

  selectRuleAll(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.selectAllChecked = isChecked;
    this.anyCheckboxSelected = isChecked;
    // Loop through your dataQualityList and update the selected state of each item
    this.dataQualityList.map(dataQuality => {
      dataQuality.selected = isChecked;
    });
  }
  

  selectRule(evt: Event, dataQuality: DataQuailtyListResponse): void {
    dataQuality.selected = (evt.target as HTMLInputElement).checked;
    if (dataQuality.selected) {
      this.ruleId = dataQuality.ruleId;
    }
    // Check if any checkbox is selected
    this.anyCheckboxSelected = this.dataQualityList.some(item => item.selected);
    // Check if all checkboxes are selected and update the "Select All" checkbox accordingly
    this.selectAllChecked = this.dataQualityList.every(item => item.selected);
  }

  public runRule(): void {
    this.dataQualityList.map(dataQuality => {
      dataQuality.selected = false;
    });
    this.anyCheckboxSelected = false;
    this.loading = true;
    const req = {
      ruleId : this.ruleId
    }
    this.ruleCreationService.runRule(this.userId, this.projectId, req).subscribe((response) => {
      this.loading = false;
      console.log("RULE API:", response);
      this.snakbar.open(response.message);
      
    }, (error) => {
      this.loading = false;
    });
  }

  public changeFilter(event: any, selectedFilter: RuleFilter): void {

    this.ruleFilter.forEach(filter => {
      if (filter !== selectedFilter) {
        filter.selected = false;
      }
    });
    selectedFilter.selected = event.target.checked;

    // Set the selected value as the name property or to null if both checkboxes are not selected
    if (this.ruleFilter.every(filter => !filter.selected)) {
      this.ruleStatus = 'Active';
    } else {
      this.ruleStatus = selectedFilter.name;
    }

    console.log("Selected value:", this.ruleStatus);
    this.getStatusRules();
  }

  public getStatusRules(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
      console.log("rules list: ", response);
      this.dataQualityList = response?.result?.content;
      this.loading = false;
    }, (error) => {
      this.loading = false;
    });
  }
  

  // pagination 
  public onPageChange(currentPage: number): void {
    this.startIndex = (currentPage - 1) * this.pageSize;
    this.endIndex = this.startIndex + this.pageSize;
  }
}
